package com.sozolab.sumon.counter.utils;

import com.sozolab.sumon.counter.model.CombinedSensorEvents;
import com.sozolab.sumon.counter.model.SensorValues;
import com.sozolab.sumon.counter.model.Checkpoints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
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
    private List<Checkpoints> checkpoints; // List<List<dynamic>>: [SV, SV/double] -> List<Checkpoints>: [SV, SV, double]
    private Timer inactivityTimer;
    public Function<String, Boolean> onActivity; // onActivity.apply(String)

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
            bodyPosture = null;
        }
    }

    public void push(List<CombinedSensorEvents> scope){
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
        inactivityTimer.scheduleAtFixedRate(repeatedResetCkpts, 500L, 3000L);
        Checkpoints ckpt = new Checkpoints(eSenseMovingAverage, new SensorValues(0, 0, 0), 0);
        checkpoints.add(ckpt);
    }

    private void prepNextCheckpoint(double data){
        if(inactivityTimer != null)
            inactivityTimer.cancel();
        inactivityTimer = new Timer("Timer");
        inactivityTimer.scheduleAtFixedRate(repeatedResetCkpts, 500L, 3000L);
        Checkpoints ckpt = new Checkpoints(eSenseMovingAverage, new SensorValues(0, 0, 0), data);
        checkpoints.add(ckpt);
    }

    private void prepNextCheckpoint(SensorValues data){
        if(inactivityTimer != null)
            inactivityTimer.cancel();
        inactivityTimer = new Timer("Timer");
        inactivityTimer.scheduleAtFixedRate(repeatedResetCkpts, 500L, 3000L);
        Checkpoints ckpt = new Checkpoints(eSenseMovingAverage, data, 0);
        checkpoints.add(ckpt);
    }

    // private class ResetCheckpoints extends TimerTask {
    //     @Override
    //     public void run(){

    //     }
    // }

    TimerTask repeatedResetCkpts = new TimerTask(){
        public void run(){
            // cancel();
            checkpoints.clear();
            compatibleActivities.clear();
            if(currentActivity != "NEUTRAL"){
                submitActivity("NEUTRAL");
            }
        }
    };
    
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
                    prepNextCheckpoint(eSenseMovingAverage.getZ() - eSenseMovingAverage.getX() - 0.15);
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
                    double situpDelta = eSenseMovingAverage.getZ() - eSenseMovingAverage.getX() - 0.15;
                    if(checkpoints.size() == 1){
                        if(Math.signum(prevDeltaType2) != Math.signum(situpDelta))
                            prepNextCheckpoint(situpDelta);
                    }else if(checkpoints.size() == 2){
                        if(Math.signum(prevDeltaType2) != Math.signum(situpDelta)){
                            prepNextCheckpoint(situpDelta);
                        }
                    }else if(checkpoints.size() == 3){
                        if(Math.signum(prevDeltaType2) != Math.signum(situpDelta)){
                            submitActivity("SITUPS");
                            checkpoints.remove(checkpoints.size()-1);
                            checkpoints.remove(checkpoints.size()-1);
                            prepNextCheckpoint(situpDelta);
                        }
                    }
                }
            }
            if(compatibleActivities.contains("PUSHUPS")){
                if(phoneMovingAverage.getZ() > -0.4){
                    compatibleActivities.remove("PUSHUPS");
                }else if(checkpoints.size() == 1){
                    prepNextCheckpoint(currentDelta);
                }else if(checkpoints.size() == 2){
                    if(Math.signum(prevDeltaType1.getY()) != Math.signum(currentDelta.getY()) && Math.abs(currentDelta.getY() - currentDelta.getX()) / 2 > 0.3) {
                        prepNextCheckpoint(currentDelta);
                    }
                }else if(checkpoints.size() == 3){
                    if(Math.signum(prevDeltaType1.getY()) != Math.signum(currentDelta.getY()) && Math.abs(currentDelta.getY() - currentDelta.getX()) / 2 > 0.3) {
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
                if(phoneMovingAverage.getZ() < -0.4){
                    compatibleActivities.remove("JUMPING_JACKS");
                }else if(checkpoints.size() == 1){
                    prepNextCheckpoint(currentDelta);
                }else if(checkpoints.size() == 2){
                    if(Math.signum(prevDeltaType1.getX()) != Math.signum(currentDelta.getX())){
                        if(Math.abs(currentDelta.getX() * (2/3) - (1/3) * currentDelta.getY()) > 0.2)
                            prepNextCheckpoint(currentDelta);
                    }   
                }else if(checkpoints.size() == 3){
                    if(Math.signum(prevDeltaType1.getX()) != Math.signum(currentDelta.getX())){
                        if(Math.abs(currentDelta.getX() * (2/3) - (1/3) * currentDelta.getY()) > 0.3)
                            prepNextCheckpoint(currentDelta);
                    }
                }else if(checkpoints.size() == 4){
                    if(Math.signum(prevDeltaType1.getX()) != Math.signum(currentDelta.getX())){
                        if(Math.abs(currentDelta.getX() * (2/3) - (1/3) * currentDelta.getY()) > 0.2)
                            prepNextCheckpoint(currentDelta);
                    }
                }else if(checkpoints.size() == 5) {
                    if(Math.signum(prevDeltaType1.getX()) != Math.signum(currentDelta.getX())){
                        if(Math.abs(currentDelta.getX() * (2/3) - (1/3) * currentDelta.getY()) > 0.3){
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
