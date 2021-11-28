package com.sozolab.sumon.counter.utils;

import com.sozolab.sumon.counter.model.CombinedSensorEvents;
import com.sozolab.sumon.counter.model.SensorValues;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.function.Function;

public class ActivityClassifier {
    
    private int eSenseWindowSize = 50;
    private double eSenseLowpassThreshold = 0.1;
    private SensorValues eSenseMovingAverage;

    private int phoneWindowSize = 80;
    private double phoneLowpassThreshold = 0.2;
    private SensorValues phoneMovingAverage;

    // activity conditions
    private String currentActivity;
    private String bodyPosture;
    private List<String> compatibleActivities;
    // private List<List<dynamic>> checkpoints = List();
    private Timer inactivityTimer;
    public Function<String, Boolean> onActivity; // onActivity.apply(String)

    public ActivityClassifier(Function<String, Boolean> onAct){
        onActivity = onAct;
        compatibleActivities = new ArrayList<>();
    }

    public void setPosture(){
        if (phoneMovingAverage.getX() + 0.3 > -phoneMovingAverage.getZ() && -phoneMovingAverage.getZ() > phoneMovingAverage.getY()){
            bodyPosture = "STANDING";
        }else if(phoneMovingAverage.getY() + 0.125 > phoneMovingAverage.getX() && phoneMovingAverage.getX() > -phoneMovingAverage.getZ()){
            bodyPosture = "CHEST_UP";
        }else if(-phoneMovingAverage.getZ() > phoneMovingAverage.getY() && -phoneMovingAverage.getZ() > phoneMovingAverage.getX()){
            bodyPosture = "CHEST_DOWN";
        }else if(phoneMovingAverage.getX() > phoneMovingAverage.getY() && phoneMovingAverage.getY() > -phoneMovingAverage.getZ()){
            bodyPosture = "KNEES_BENT";
        }else{
            bodyPosture = null;
        }
    }

    public void push(List<CombinedSensorEvents> list){
        //DO SOMETHING
    }

    private void submitActivity(String name){
        currentActivity = name;
        if(name != "NEUTRAL"){
            compatibleActivities.add(name);
        }
    }

    public void classifyActivity(){

    }
}
