package com.sozolab.sumon;

public class Activity {

    private String activityName;
    private String startTime;
    private String stopTime;
    private String duration;
    private Integer counterNum;

    public Activity(){

    }

    public Activity(String activityName, String startTime, String stopTime, String duration, Integer counterNum){
        this.activityName = activityName;
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.duration = duration;
        this.counterNum = counterNum;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getStopTime() {
        return stopTime;
    }

    public void setStopTime(String stopTime) {
        this.stopTime = stopTime;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public Integer getCounter() { return counterNum; }

    public void setCounter(Integer counterNum) { this.counterNum = counterNum; }
}
