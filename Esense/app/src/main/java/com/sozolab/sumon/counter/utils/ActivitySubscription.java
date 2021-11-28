package com.sozolab.sumon.counter.utils;

import com.sozolab.sumon.counter.model.CombinedSensorEvents;
import com.sozolab.sumon.counter.model.SensorValues;
import com.sozolab.sumon.io.esense.esenselib.ESenseSensorListener;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ActivitySubscription {
    private Queue<CombinedSensorEvents> mSensorEvents;
    private ActivityClassifier activityClassifier;
    public ActivitySubscription(){
        mSensorEvents = new LinkedList<>();
    }
}

public void submitEvent(CombinedSensorEvents event){
    mSensorEvents.add(event);
    if(mSensorEvents.){
        mSensorEvents.remove();
        activityClassifier.push((List) mSensorEvents);
    }
}