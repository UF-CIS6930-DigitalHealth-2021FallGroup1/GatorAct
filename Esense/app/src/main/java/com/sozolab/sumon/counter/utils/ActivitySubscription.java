package com.sozolab.sumon.counter.utils;

import android.hardware.SensorEvent;
import android.util.Log;

import com.sozolab.sumon.counter.model.CombinedSensorEvents;
import com.sozolab.sumon.counter.model.SensorValues;

import com.sozolab.sumon.io.esense.esenselib.ESenseEvent;
import com.sozolab.sumon.io.esense.esenselib.ESenseSensorListener;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;

public class ActivitySubscription {
    private final String TAG = "ActivitySubscription";
    private Queue<CombinedSensorEvents> mSensorEvents;
    private ActivityClassifier activityClassifier;

    private CombinedSensorEvents combinedEvent;
    private int bufferSize = 100;

    public ActivitySubscription(Function<String, Boolean> onActivity){
        mSensorEvents = new LinkedList<>();
        combinedEvent = new CombinedSensorEvents();
        activityClassifier = new ActivityClassifier(onActivity);
    }

    public void submitEvent(CombinedSensorEvents event){
        mSensorEvents.add(event);
        if(mSensorEvents.size() > bufferSize){
            mSensorEvents.remove();
            activityClassifier.push((List) mSensorEvents);
        }
    }

    public void updatePhoneAcc(double evtX, double evtY, double evtZ) {
        String phoneSensorData = "Phone accel x: " + evtX + ", y: " + evtY + ", z: " + evtZ;
        Log.d(TAG, phoneSensorData);
        int divideConstant = 10;
        combinedEvent.mPhone.update(evtX/divideConstant, evtY/divideConstant, evtZ/divideConstant);
        submitEvent(combinedEvent);
    }

    public void updateEsenseAcc(double evtX, double evtY, double evtZ) {
        String eSensorData = "Phone accel x: " + evtX + ", y: " + evtY + ", z: " + evtZ;
        Log.d(TAG, eSensorData);
        int divideConstant = 10000;
        combinedEvent.mPhone.update(evtX/divideConstant, evtY/divideConstant, evtZ/divideConstant);
        submitEvent(combinedEvent);
    }
}