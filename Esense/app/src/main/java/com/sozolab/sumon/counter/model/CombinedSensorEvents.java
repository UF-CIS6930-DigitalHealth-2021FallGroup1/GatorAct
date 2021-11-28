package com.sozolab.sumon.counter.model;

import java.util.Date;

public class CombinedSensorEvents {
    
    private Date mTimestamp;
    private SensorValues mPhone;
    private SensorValues mESense;
    
    public void CombinedSensorEvents(){
        mPhone = new SensorValues(0, 0, 0);
        mESense = new SensorValues(0, 0, 0);
        mTimestamp = Date.now();
    }

    public void CombinedSensorEvents(double pX, double pY, doube pZ, double eX, double eY, double eZ){
        mPhone = new SensorValues(pX, pY, pZ);
        mESense = new SensorValues(eX, eY, eZ);
        mTimestamp = Date.now();
    }

    //onUpdate()

    //toString()
}
