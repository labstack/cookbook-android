package com.labstack.cookbook.android.pushnotification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.iid.InstanceID;
import com.labstack.MessageConnectHandler;
import com.labstack.MessageDataHandler;
import com.labstack.android.Client;
import com.labstack.android.Message;

import java.util.concurrent.ThreadLocalRandom;

public class MainActivity extends AppCompatActivity {
    protected Client client;
    protected Message message;
    private String clientId;
    private NotificationManager notificationManager;
    private int notificationId;

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
                message.subscribe("broadcast", false);
            }
        });
        message.onMessage(new MessageDataHandler() {
            @Override
            public void handle(String topic, byte[] data) {
                // Notify
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                Notification notification = new Notification.Builder(MainActivity.this)
                        .setSmallIcon(R.drawable.ic_push_notification)
                        .setContentTitle("Broadcast")
                        .setContentText(new String(data))
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
