package com.yq.miclock;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private final String TAG = this.getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test);
        TemperatureView view = findViewById(R.id.tempView);
        view.setCurrentTemp(120);
        view.setTemperatureListener(temp -> Log.i(TAG, "onCreate: " + temp));
    }
}
