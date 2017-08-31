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
import com.labstack.ConnectConnectionHandler;
import com.labstack.ConnectMessageHandler;
import com.labstack.android.Client;
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
    protected com.labstack.android.Connect connect;
    private String clientId;
    private LocationManager locationManager;
    private Map<String, Message> devices = new HashMap<>();
    private Moshi moshi = new Moshi.Builder().add(Date.class, new Rfc3339DateJsonAdapter().nullSafe()).build();
    private JsonAdapter<Message> messageJsonAdapter = moshi.adapter(Message.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize LabStack connect service
        client = new Client(this, "<ACCOUNT_ID>", "<API_KEY>");
        clientId = InstanceID.getInstance(this).getId();
        connect = client.connect(clientId);
        connect.onConnect(new ConnectConnectionHandler() {
            @Override
            public void handle(boolean reconnect, String serverURI) {
                connect.subscribe("tracker");
                @SuppressLint("MissingPermission")
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                publishMessage(location);
            }
        });
        connect.onMessage(new ConnectMessageHandler() {
            @Override
            public void handle(String topic, byte[] payload) {
                try {
                    Message message = messageJsonAdapter.fromJson(new String(payload));
                    devices.put(message.getDeviceId(), message);
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
                publishMessage(location);
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

    private void publishMessage(Location location) {
        Message message = new Message(clientId, location);
        String json = messageJsonAdapter.toJson(message);
        connect.publish("tracker", json.getBytes());
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

        for (Map.Entry<String, Message> entry : devices.entrySet()) {
            Message message = entry.getValue();

            table.setColumnStretchable(0, true);
            table.setColumnStretchable(1, true);
            table.setColumnStretchable(2, true);
            table.setColumnStretchable(3, true);

            // Body
            row = new TableRow(this);
            row.setPadding(16, 16, 16, 16);
            timeDevice = new TextView(this);
            timeDevice.setText(dateFormatter.format(message.getTime()) + "\n" + message.getDeviceId());
            location = new TextView(this);
            if (Geocoder.isPresent()) {
                Geocoder coder = new Geocoder(this);
                try {
                    List<Address> addresses = coder.getFromLocation(message.getLatitude(), message.getLongitude(), 1);
                    if (addresses.size() == 1) {
                        Address address = addresses.get(0);
                        location.setText(address.getLocality());
                    }
                } catch (IOException e) {
                    Log.w(MainActivity.tag, e.getMessage());
                }
            }
            altitudeSpeed = new TextView(this);
            altitudeSpeed.setText(String.format("%.2f m\n%.2f m/s", message.getAltitude(), message.getSpeed()));
            row.addView(timeDevice);
            row.addView(location);
            row.addView(altitudeSpeed);
            table.addView(row);


            if (message.getDeviceId().equals(clientId)) {
                row.setBackgroundColor(Color.YELLOW);
            }
        }
    }
}
