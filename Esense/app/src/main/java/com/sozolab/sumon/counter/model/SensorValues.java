package com.sozolab.sumon.counter.model;

public class SensorValues {
    private double mX;
    private double mY;
    private double mZ;
    public void SensorValues(double x, double y, double z){
        mX = x;
        mY = y;
        mZ = z;
    }
    public void SensorValues(double[] list){
        mX = list[0];
        mY = list[1];
        mZ = list[2];
    }
    public void update(double x, double y, double z){
        mX = x;
        mY = y;
        mZ = z;
    }
    public double getX(){
        return mX;
    }
    public double getY(){
        return mY;
    }
    public double getZ(){
        return mZ;
    }
    // public toString(){

    // }
    // public toList(){

    // }

    // operators: +, -, /, abs()
    // > (larger than): x || y || z
    // == (equals to): x && y && z
}
