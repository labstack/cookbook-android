package com.labstack.cookbook.android.errorreporting;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.iid.InstanceID;
import com.labstack.Fields;
import com.labstack.android.Client;
import com.labstack.android.Log;
import com.labstack.cookbook.android.R;

public class MainActivity extends AppCompatActivity {
    private Client client;
    private Log log;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize LabStack client and log service
        client = new Client(this, "<ACCOUNT_ID>", "<API_KEY>");
        log = client.log();
        log.setDispatchInterval(5);
        log.getFields()
                .add("app_name", "error-reporting")
                .add("app_id", InstanceID.getInstance(this).getId())
                .add("device_type", "Android")
                .add("device_release", Build.VERSION.RELEASE)
                .add("device_sdk", Build.VERSION.SDK_INT);

        // Automatically report crash (fatal error)
        Button buttonCrash = (Button) findViewById(R.id.button_crash);
        buttonCrash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                throw new RuntimeException("fatal error");
            }
        });

        // Manually report non-fatal error
        Button buttonError = (Button) findViewById(R.id.button_error);
        buttonError.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    throw new Exception("non-fatal error");
                } catch (Exception e) {
                    log.error(new Fields().add("message", e.getMessage()));
                }
            }
        });
    }
}
