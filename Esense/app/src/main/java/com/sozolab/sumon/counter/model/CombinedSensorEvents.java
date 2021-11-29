package com.sozolab.sumon.counter.model;

import java.util.Date;

public class CombinedSensorEvents {
    
    private Date mTimestamp;
    public SensorValues mPhone;
    public SensorValues mESense;
    
    public CombinedSensorEvents(){
        mPhone = new SensorValues(0, 0, 0);
        mESense = new SensorValues(0, 0, 0);
        mTimestamp = new Date();
    }

    public CombinedSensorEvents(double pX, double pY, double pZ, double eX, double eY, double eZ){
        mPhone = new SensorValues(pX, pY, pZ);
        mESense = new SensorValues(eX, eY, eZ);
        mTimestamp = new Date();
    }

    public SensorValues getPhoneEvent(){
        return mPhone;
    }

    public SensorValues getESenseEvent(){
        return mESense;
    }

    //onUpdate()

    //toString()
}
