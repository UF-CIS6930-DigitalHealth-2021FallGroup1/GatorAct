package com.sozolab.sumon.counter.utils;

import android.util.Log;

import com.sozolab.sumon.counter.model.CombinedSensorEvents;
import com.sozolab.sumon.counter.model.SensorValues;
import com.sozolab.sumon.counter.model.Checkpoints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;

public class ActivityClassifier {
    private final String TAG = "ActivityClassifier";
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
    private List<Checkpoints> checkpoints; // List<List<dynamic>>: [SV, SV/double] -> List<Checkpoints>: [SV, SV, double]
    private Timer inactivityTimer;
    public Function<String, Boolean> onActivity; // onActivity.apply(String)

    // Task Period
    private long timerDuration = 3000L;
    private long timeDelay = 500L;

    public ActivityClassifier(Function<String, Boolean> onAct){
        onActivity = onAct;
        compatibleActivities = new ArrayList<>();
        checkpoints = new ArrayList<>();
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
            bodyPosture = "";
        }
    }

//    public void push(List<CombinedSensorEvents> scope){
//    public void push(Queue<CombinedSensorEvents> scopeQ){
    public void push(CombinedSensorEvents[] scopeArr){
        List<CombinedSensorEvents> scope = new ArrayList<>(Arrays.asList(scopeArr));
//        List<CombinedSensorEvents> scope = (List) scopeQ;
        Collections.reverse(scope);
        List<CombinedSensorEvents> cropped = scope.subList(0, eSenseWindowSize); // take(eSenseWindowSize)
        SensorValues result = new SensorValues(0, 0, 0);
        for(CombinedSensorEvents events : cropped){
            result.incrementBy(events.getESenseEvent()); //map().reduce()
        }
        result.dividedBy((double) eSenseWindowSize);

        // eSense data changed
        boolean anyChange = false;
        if(eSenseMovingAverage == null || result.subtract(eSenseMovingAverage).abs().largerThan(eSenseLowpassThreshold)){
            eSenseMovingAverage = result;
            anyChange = true;
        }

        if(phoneMovingAverage == null || !anyChange){
            List<CombinedSensorEvents> croppedPhone = scope.subList(0, phoneWindowSize);
            SensorValues phoneRes = new SensorValues(0, 0, 0);
            for(CombinedSensorEvents events : croppedPhone){
                phoneRes.incrementBy(events.getPhoneEvent()); //map().reduce()
            }
            phoneRes.dividedBy((double) phoneWindowSize);
            result = phoneRes;
            // phone data changed
            if(phoneMovingAverage == null || result.subtract(phoneMovingAverage).abs().largerThan(phoneLowpassThreshold)){
                phoneMovingAverage = result;
                anyChange = true;
                setPosture();
            }
        }

        // something changed => check for activity
        if(anyChange){
            classifyActivity();
        }

    }

    private void submitActivity(String name){
        currentActivity = name;
        onActivity.apply(name);
        if(name != "NEUTRAL"){
            compatibleActivities.add(name);
        }
    }

    private void prepNextCheckpoint(){
        if(inactivityTimer != null)
            inactivityTimer.cancel();
        inactivityTimer = new Timer("Timer");
        inactivityTimer.scheduleAtFixedRate(repeatedResetCkpts(), timeDelay, timerDuration);
        Checkpoints ckpt = new Checkpoints(eSenseMovingAverage, new SensorValues(0, 0, 0), 0);
        checkpoints.add(ckpt);
    }

    private void prepNextCheckpoint(double data){
        if(inactivityTimer != null)
            inactivityTimer.cancel();
        inactivityTimer = new Timer("Timer");
        inactivityTimer.scheduleAtFixedRate(repeatedResetCkpts(), timeDelay, timerDuration);
        Checkpoints ckpt = new Checkpoints(eSenseMovingAverage, new SensorValues(0, 0, 0), data);
        checkpoints.add(ckpt);
    }

    private void prepNextCheckpoint(SensorValues data){
        if(inactivityTimer != null)
            inactivityTimer.cancel();
        inactivityTimer = new Timer("Timer");
        inactivityTimer.scheduleAtFixedRate(repeatedResetCkpts(), timeDelay, timerDuration);
        Checkpoints ckpt = new Checkpoints(eSenseMovingAverage, data, 0);
        checkpoints.add(ckpt);
    }

    // private class ResetCheckpoints extends TimerTask {
    //     @Override
    //     public void run(){

    //     }
    // }

    private TimerTask repeatedResetCkpts() {
        return new TimerTask(){
            @Override
            public void run(){
                // cancel();
                checkpoints.clear();
                compatibleActivities.clear();
                if(currentActivity != "NEUTRAL"){
                    submitActivity("NEUTRAL");
                }
            }
        };
    }
    
    private void resetCheckpoints(){
        // timer.cancel();
        checkpoints.clear();
        compatibleActivities.clear();
        if(currentActivity != "NEUTRAL"){
            submitActivity("NEUTRAL");
        }
    }

    public void classifyActivity(){
        if(compatibleActivities.isEmpty()){
            resetCheckpoints();
        }

        if(checkpoints.isEmpty()){
            Log.d(TAG, "Add activity according to bodyPosture:"+ bodyPosture);
            switch(bodyPosture){
                case "KNEES_BENT":
                    compatibleActivities.add("SQUATS");
                    prepNextCheckpoint();
                    break;
                case "STANDING":
                    compatibleActivities.add("JUMPING_JACKS");
                    prepNextCheckpoint();
                    break;
                case "CHEST_DOWN":
                    compatibleActivities.add("PUSHUPS");
                    prepNextCheckpoint();
                    break;
                case "CHEST_UP":
                    compatibleActivities.add("SITUPS");
//                    prepNextCheckpoint(eSenseMovingAverage.getY() - eSenseMovingAverage.getX() - 0.15);
//                    prepNextCheckpoint(eSenseMovingAverage.getY());
//                    prepNextCheckpoint(eSenseMovingAverage);
                    prepNextCheckpoint();
                    break;
                default:
                    return;
            }
        }else{
            SensorValues prevESenseMovAvg = (SensorValues) checkpoints.get(checkpoints.size()-1).getMA();
            SensorValues prevDeltaType1 = (SensorValues) checkpoints.get(checkpoints.size()-1).getDelta1();
            double prevDeltaType2 = (double) checkpoints.get(checkpoints.size()-1).getDelta2();
            SensorValues currentDelta = eSenseMovingAverage.subtract(prevESenseMovAvg);
            if(compatibleActivities.contains("SITUPS")){
                if(bodyPosture != "CHEST_UP"){
                    compatibleActivities.remove("SITUPS");
                }else{
                    // double situpDelta = eSenseMovingAverage.getZ() - eSenseMovingAverage.getX() - 0.5;
//                    double situpDelta = eSenseMovingAverage.getY() - eSenseMovingAverage.getX() - 0.15;
                    double situpDelta = eSenseMovingAverage.getY();
                    Log.d(TAG, "eSenseMovingAverage.z: " + eSenseMovingAverage.getZ());
                    Log.d(TAG, "eSenseMovingAverage.y: " + eSenseMovingAverage.getY());
                    Log.d(TAG, "eSenseMovingAverage.x: " + eSenseMovingAverage.getX());
                    Log.d(TAG, "prevDeltaType2: " + prevDeltaType2);
                    if(checkpoints.size() == 1){
//                        Log.d(TAG, "SITUPS Start");
                        prepNextCheckpoint(currentDelta);
//                        if(Math.signum(prevDeltaType2) != Math.signum(situpDelta))
//                        if(Math.signum(prevDeltaType1.getY()) != Math.signum(currentDelta.getY()))
//                            prepNextCheckpoint(currentDelta);
//                            prepNextCheckpoint(situpDelta);
                    }else if(checkpoints.size() == 2){
//                        Log.d(TAG, "SITUPS 2nd ckpt");
                        if(Math.signum(prevDeltaType1.getY()) != Math.signum(currentDelta.getY())) {
//                        if(Math.signum(prevDeltaType2) != Math.signum(situpDelta)){
//                            Log.d(TAG, "SITUPS submitted");
//                            submitActivity("SITUPS");
//                            checkpoints.remove(checkpoints.size()-1);
//                            prepNextCheckpoint(situpDelta);
                            prepNextCheckpoint(currentDelta);
                        }
                    }else if(checkpoints.size() == 3){
                        Log.d(TAG, "SITUPS 3rd ckpt");
                        if(Math.signum(prevDeltaType1.getY()) != Math.signum(currentDelta.getY()) || Math.signum(prevDeltaType1.getX()) != Math.signum(currentDelta.getX())) {
//                        if(Math.signum(prevDeltaType2) != Math.signum(situpDelta)){
                            Log.d(TAG, "SITUPS submitted");
                            submitActivity("SITUPS");
                            checkpoints.remove(checkpoints.size()-1);
                            checkpoints.remove(checkpoints.size()-1);
//                            prepNextCheckpoint(situpDelta);
                            prepNextCheckpoint(currentDelta);
                        }
                    }
                }
            }
            if(compatibleActivities.contains("PUSHUPS")){
                Log.d(TAG, "checkPointSize: " + checkpoints.size() + ", phoneMovingAverage.getZ():" + phoneMovingAverage.getZ());
                if(phoneMovingAverage.getZ() > -0.4){
//                    Log.d(TAG, "remove PUSHUPS");
                    compatibleActivities.remove("PUSHUPS");
                }else if(checkpoints.size() == 1){
//                    Log.d(TAG, "PUSHUPS Start");
                    prepNextCheckpoint(currentDelta);
                }
                else if(checkpoints.size() == 2){
//                    Log.d(TAG, "PUSHUPS Second Checkout");
                    Log.d(TAG, "prevDeltaType1.y" + prevDeltaType1.getY());
                    Log.d(TAG, "currentDelta.y" + currentDelta.getY());
                    Log.d(TAG, "currentDelta.x" + currentDelta.getX());
                    if(Math.signum(prevDeltaType1.getY()) != Math.signum(currentDelta.getY()) && Math.abs(currentDelta.getY() - currentDelta.getX()) / 2 > 0.03) {
//                    if(Math.signum(prevDeltaType1.getX()) != Math.signum(currentDelta.getX()) && Math.abs(currentDelta.getX()) > 0.09) {
                        prepNextCheckpoint(currentDelta);
                    }
                }
                else if(checkpoints.size() == 3){
//                    Log.d(TAG, "PUSHUPS Third Checkout");
                    if(Math.signum(prevDeltaType1.getY()) != Math.signum(currentDelta.getY()) && Math.abs(currentDelta.getY() - currentDelta.getX()) / 2 > 0.03) {
//                    if(Math.signum(prevDeltaType1.getX()) != Math.signum(currentDelta.getX()) && Math.abs(currentDelta.getX()) > 0.09) {
                        submitActivity("PUSHUPS");
                        checkpoints.remove(checkpoints.size()-1);
                        checkpoints.remove(checkpoints.size()-1);
                        prepNextCheckpoint(currentDelta);
                    }
                }
            }
            if(compatibleActivities.contains("SQUATS")){
                if(checkpoints.size() == 1) {
                    // CHEST UP: knees are bent over 90 deg, same leg position as sit-ups
                    if((bodyPosture == "KNEES_BENT" || bodyPosture == "CHEST_UP")){
                        prepNextCheckpoint();  // give sit-ups a chance to detect
                    }else {
                        if(bodyPosture != "STANDING"){
                            compatibleActivities.remove("SQUATS");
                        }
                    }
                }else if(checkpoints.size() == 2){
                    if(bodyPosture == "STANDING"){
                        submitActivity("SQUATS");
                        checkpoints.remove(checkpoints.size()-1);
                    }else if(bodyPosture != "KNEES_BENT" && bodyPosture != "CHEST_UP"){
                        compatibleActivities.remove("SQUATS");
                    }
                }
            }
            if(compatibleActivities.contains("JUMPING_JACKS")){
                Log.d(TAG, "phoneMovingAverage.z: " + phoneMovingAverage.getZ());
                if(phoneMovingAverage.getZ() < -0.4){
                    compatibleActivities.remove("JUMPING_JACKS");
                }else if(checkpoints.size() == 1){
                    prepNextCheckpoint(currentDelta);
                }else if(checkpoints.size() == 2){
                    if(Math.signum(prevDeltaType1.getX()) != Math.signum(currentDelta.getX())){
                        if(Math.abs(currentDelta.getX() * (2/3) - (1/3) * currentDelta.getY()) > 0.1)
                            prepNextCheckpoint(currentDelta);
                    }   
                }else if(checkpoints.size() == 3){
                    if(Math.signum(prevDeltaType1.getX()) != Math.signum(currentDelta.getX())){
                        if(Math.abs(currentDelta.getX() * (2/3) - (1/3) * currentDelta.getY()) > 0.15)
                            prepNextCheckpoint(currentDelta);
                    }
                }else if(checkpoints.size() == 4){
                    if(Math.signum(prevDeltaType1.getX()) != Math.signum(currentDelta.getX())){
                        if(Math.abs(currentDelta.getX() * (2/3) - (1/3) * currentDelta.getY()) > 0.1)
                            prepNextCheckpoint(currentDelta);
                    }
                }else if(checkpoints.size() == 5) {
                    if(Math.signum(prevDeltaType1.getX()) != Math.signum(currentDelta.getX())){
                        if(Math.abs(currentDelta.getX() * (2/3) - (1/3) * currentDelta.getY()) > 0.15){
                            submitActivity("JUMPING_JACKS");
                            checkpoints.remove(checkpoints.size()-1);
                            checkpoints.remove(checkpoints.size()-1);
                            checkpoints.remove(checkpoints.size()-1);
                            checkpoints.remove(checkpoints.size()-1);
                            prepNextCheckpoint(currentDelta);
                        }
                    }
                }
            }
        }
    }
}
