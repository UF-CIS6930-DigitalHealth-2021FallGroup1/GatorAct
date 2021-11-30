package com.sozolab.sumon;

import static androidx.core.content.ContextCompat.getSystemService;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.sozolab.sumon.counter.utils.ActivitySubscription;
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

    ActivitySubscription activitySubscription;
    public PhoneSensorListenerManager(Context context, ActivitySubscription activitySubscription) {
        this.context = context;
        this.activitySubscription = activitySubscription;
        activityName = "";
        dataCollecting = false;
        mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);
        Log.d(TAG, "PhoneSensorListenerManager Create");
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
    public void onSensorChanged(SensorEvent evt) {
        if(dataCollecting) {
            if (evt.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                this.activitySubscription.updatePhoneAcc(evt.values[0], evt.values[1], evt.values[2]);
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
