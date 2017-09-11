package com.labstack.cookbook.android.locationtracker;

import android.location.Location;

import java.util.Date;

public class Payload {
    private Date time;
    private String deviceId;
    private double latitude;
    private double longitude;
    private double altitude;
    private float speed;

    public Payload(String deviceId, Location location) {
        time = new Date();
        this.deviceId = deviceId;
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            altitude = location.getAltitude();
            speed = location.getSpeed();
        }
    }

    public Date getTime() {
        return time;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public float getSpeed() {
        return speed;
    }
}
