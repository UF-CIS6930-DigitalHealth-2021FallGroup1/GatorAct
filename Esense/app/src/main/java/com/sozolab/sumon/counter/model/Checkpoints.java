package com.sozolab.sumon.counter.model;

public class Checkpoints {
    
    private SensorValues movAvg;
    private SensorValues deltaType1;
    private double deltaType2;
    
    public Checkpoints(SensorValues movingAverage, SensorValues delta1, double delta2){
        movAvg = movingAverage;
        deltaType1 = delta1;
        deltaType2 = delta2;
    }

    public SensorValues getMA(){
        return movAvg;
    }
    public SensorValues getDelta1(){
        return deltaType1;
    }
    public double get getDelta2(){
        return deltaType2;
    }
}
