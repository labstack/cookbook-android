package com.labstack.cookbook.android.locationtracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.android.gms.iid.InstanceID;
import com.labstack.MessageConnectHandler;
import com.labstack.MessageDataHandler;
import com.labstack.android.Client;
import com.labstack.android.Message;
import com.labstack.cookbook.android.R;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Rfc3339DateJsonAdapter;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String tag = "location-tracker";
    private static DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    protected Client client;
    protected Message message;
    private String clientId;
    private LocationManager locationManager;
    private Map<String, Payload> devices = new HashMap<>();
    private Moshi moshi = new Moshi.Builder().add(Date.class, new Rfc3339DateJsonAdapter().nullSafe()).build();
    private JsonAdapter<Payload> payloadJsonAdapter = moshi.adapter(Payload.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize LabStack message service
        client = new Client(this, "<ACCOUNT_ID>", "<API_KEY>");
        clientId = InstanceID.getInstance(this).getId();
        message = client.message(clientId);
        message.onConnect(new MessageConnectHandler() {
            @Override
            public void handle() {
                message.subscribe("tracker", false);
                @SuppressLint("MissingPermission")
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                publishLocation(location);
            }
        });
        message.onMessage(new MessageDataHandler() {
            @Override
            public void handle(String topic, byte[] data) {
                try {
                    Payload payload = payloadJsonAdapter.fromJson(new String(data));
                    devices.put(payload.getDeviceId(), payload);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateUi();
                        }
                    });
                } catch (IOException e) {
                    Log.e(MainActivity.tag, e.getMessage());
                }
            }
        });

        // Initialize location
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                publishLocation(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        // Request permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    0);
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 4, locationListener);
    }

    private void publishLocation(Location location) {
        Payload payload = new Payload(clientId, location);
        String json = payloadJsonAdapter.toJson(payload);
        message.publish("tracker", json.getBytes());
    }

    private void updateUi() {
        TableLayout table = (TableLayout) findViewById(R.id.table);
        table.removeAllViews();

        // Header
        TableRow row = new TableRow(this);
        row.setPadding(16, 16, 16, 16);
        row.setBackgroundColor(Color.parseColor("#00AFD1"));
        TextView timeDevice = new TextView(this);
        timeDevice.setTextColor(Color.WHITE);
        timeDevice.setText("Time\nDevice ID");
        TextView location = new TextView(this);
        location.setTextColor(Color.WHITE);
        location.setText("Location");
        TextView altitudeSpeed = new TextView(this);
        altitudeSpeed.setTextColor(Color.WHITE);
        altitudeSpeed.setText("Altitude\nSpeed");
        row.addView(timeDevice);
        row.addView(location);
        row.addView(altitudeSpeed);
        table.addView(row);

        for (Map.Entry<String, Payload> entry : devices.entrySet()) {
            Payload payload = entry.getValue();

            table.setColumnStretchable(0, true);
            table.setColumnStretchable(1, true);
            table.setColumnStretchable(2, true);
            table.setColumnStretchable(3, true);

            // Body
            row = new TableRow(this);
            row.setPadding(16, 16, 16, 16);
            timeDevice = new TextView(this);
            timeDevice.setText(dateFormatter.format(payload.getTime()) + "\n" + payload.getDeviceId());
            location = new TextView(this);
            if (Geocoder.isPresent()) {
                Geocoder coder = new Geocoder(this);
                try {
                    List<Address> addresses = coder.getFromLocation(payload.getLatitude(), payload.getLongitude(), 1);
                    if (addresses.size() == 1) {
                        Address address = addresses.get(0);
                        location.setText(address.getLocality());
                    }
                } catch (IOException e) {
                    Log.w(MainActivity.tag, e.getMessage());
                }
            }
            altitudeSpeed = new TextView(this);
            altitudeSpeed.setText(String.format("%.2f m\n%.2f m/s", payload.getAltitude(), payload.getSpeed()));
            row.addView(timeDevice);
            row.addView(location);
            row.addView(altitudeSpeed);
            table.addView(row);


            if (payload.getDeviceId().equals(clientId)) {
                row.setBackgroundColor(Color.YELLOW);
            }
        }
    }
}
