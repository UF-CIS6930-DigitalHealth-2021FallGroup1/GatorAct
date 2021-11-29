package com.sozolab.sumon;

import static androidx.core.content.ContextCompat.getSystemService;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.sozolab.sumon.io.esense.esenselib.ESenseConfig;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PhoneSensorListenerManager implements SensorEventListener {
    private final String TAG = "PhoneSensorListenerManager";
    private final SensorManager mSensorManager;
    private final Sensor mAccelerometer;
    private boolean dataCollecting;

    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 600;

    Context context;
    String activityName;

    public PhoneSensorListenerManager(Context context) {
        Log.d(TAG, "PhoneSensorListenerManager Create");
        this.context = context;
        activityName = "";
        dataCollecting = false;
        mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);
    }

//    protected void onResume() {
//        super.onResume();
//        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
//    }
//
//    protected void onPause() {
//        super.onPause();
//        mSensorManager.unregisterListener(this);
//    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(dataCollecting) {
            Log.d(TAG, "Phone onSensorChanged dataCollecting start");
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                String sensorData = "Accel x: " + event.values[0] + ", y: " + event.values[1] + ", z: " + event.values[2];
                Log.d(TAG, sensorData);
            }
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void startDataCollection(String activity) {
        this.activityName = activity;
        dataCollecting = true;
    }

    public void stopDataCollection(){
        dataCollecting = false;
    }
}
