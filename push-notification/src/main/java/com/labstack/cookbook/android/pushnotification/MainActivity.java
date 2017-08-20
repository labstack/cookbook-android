package com.labstack.cookbook.android.pushnotification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.iid.InstanceID;
import com.labstack.MqttConnectHandler;
import com.labstack.MqttMessageHandler;
import com.labstack.android.Client;
import com.labstack.android.Mqtt;
import com.labstack.cookbook.android.pushnotification.R;

import java.util.concurrent.ThreadLocalRandom;

public class MainActivity extends AppCompatActivity {
    protected Client client;
    protected Mqtt mqtt;
    private String clientId;
    private NotificationManager notificationManager;
    private int notificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize LabStack mqtt service
        client = new Client(this, "ie8t5fgcb6s2vaxgg02y", "VouXFKK2A1TkuMUVz3wV2zvmapIdRuFM");
        clientId = InstanceID.getInstance(this).getId();
        mqtt = client.mqtt(clientId);
        mqtt.onConnect(new MqttConnectHandler() {
            @Override
            public void handle(boolean reconnect, String serverURI) {
                mqtt.subscribe("broadcast");
            }
        });
        mqtt.onMessage(new MqttMessageHandler() {
            @Override
            public void handle(String topic, byte[] payload) {
                // Notify
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                Notification notification = new Notification.Builder(MainActivity.this)
                        .setSmallIcon(R.drawable.ic_push_notification)
                        .setContentTitle("Broadcast")
                        .setContentText(new String(payload))
                        .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                        .setContentIntent(pendingIntent)
                        .build();
                notificationManager.notify(notificationId, notification);
            }
        });

        // Initialize notification
        notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationId = ThreadLocalRandom.current().nextInt();
    }
}
