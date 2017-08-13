package com.labstack.cookbook.android.crashreporter;

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

        // Initialize log service
        client = new Client(getApplicationContext(), "<ACCOUNT_ID>", "<API_KEY>");
        log = client.log();
        log.setDispatchInterval(5);
        log.getFields()
                .add("app_name", "crash-reporter")
                .add("app_id", InstanceID.getInstance(this).getId());

        // Automatically report crash (fatal error)
        Button crashButton = (Button) findViewById(R.id.crash_button);
        crashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                throw new RuntimeException("fatal error");
            }
        });

        // Manually report non-fatal error
        Button errorButton = (Button) findViewById(R.id.error_button);
        errorButton.setOnClickListener(new View.OnClickListener() {
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
