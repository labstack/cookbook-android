package com.labstack.cookbook.android.crashreporter;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.iid.InstanceID;
import com.labstack.Fields;
import com.labstack.Log;
import com.labstack.android.Client;
import com.labstack.cookbook.android.R;

import java.io.PrintWriter;
import java.io.StringWriter;

public class MainActivity extends AppCompatActivity {
    private Client client;
    private Log log;
    private Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandle;

    private static String getStackTrace(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        return stringWriter.toString();
    }

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

        // Automatically report uncaught fatal error
        defaultUncaughtExceptionHandle = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                log.fatal(new Fields()
                        .add("message", throwable.getMessage())
                        .add("stack_trace", getStackTrace(throwable)));
                defaultUncaughtExceptionHandle.uncaughtException(thread, throwable);
            }
        });

        // Simulate crash
        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                throw new RuntimeException("fatal error");
            }
        });

        // Manually report non-fatal error
        try {
            throw new Exception("non-fatal error");
        } catch (Exception e) {
            log.error(new Fields().add("message", e.getMessage()));
        }
    }
}
