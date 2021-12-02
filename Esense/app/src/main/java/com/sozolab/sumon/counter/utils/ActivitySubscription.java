package com.sozolab.sumon.counter.utils;

import android.hardware.SensorEvent;
import android.util.Log;

import com.sozolab.sumon.counter.model.CombinedSensorEvents;
import com.sozolab.sumon.counter.model.SensorValues;

import com.sozolab.sumon.io.esense.esenselib.ESenseEvent;
import com.sozolab.sumon.io.esense.esenselib.ESenseSensorListener;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

public class ActivitySubscription {
    private final String TAG = "ActivitySubscription";
    private Queue<CombinedSensorEvents> mSensorEvents;
//    Iterator<CombinedSensorEvents> listIterator;
    private ActivityClassifier activityClassifier;

    private CombinedSensorEvents combinedEvent;
    private int bufferSize = 100;

    public ActivitySubscription(Function<String, Boolean> onActivity){
        mSensorEvents = new LinkedBlockingQueue<>();
        combinedEvent = new CombinedSensorEvents();
        activityClassifier = new ActivityClassifier(onActivity);
    }

    public void submitEvent(CombinedSensorEvents event){
        mSensorEvents.add(event);
        if(mSensorEvents.size() > bufferSize){
            mSensorEvents.remove();
            activityClassifier.push(mSensorEvents.toArray(new CombinedSensorEvents[bufferSize]));
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
        String eSensorData = "eSense accel x: " + evtX + ", y: " + evtY + ", z: " + evtZ;
        Log.d(TAG, eSensorData);
        int divideConstant = 1;
        combinedEvent.mESense.update(evtX/divideConstant, evtY/divideConstant, evtZ/divideConstant);
        submitEvent(combinedEvent);
    }
}