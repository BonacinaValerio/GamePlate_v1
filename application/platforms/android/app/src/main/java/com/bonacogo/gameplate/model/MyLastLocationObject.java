package com.bonacogo.gameplate.model;

import java.io.Serializable;

public class MyLastLocationObject implements Serializable {
    public static final String DETAILS_STRING = "MY_LAST_LOCATION";
    private double latitude, longitude;
    private float zoom, bearing, tilt;

    public MyLastLocationObject(double latitude, double longitude, float zoom, float bearing, float tilt) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.zoom = zoom;
        this.bearing = bearing;
        this.tilt = tilt;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public float getZoom() {
        return zoom;
    }

    public float getBearing() {
        return bearing;
    }

    public float getTilt() {
        return tilt;
    }
}
