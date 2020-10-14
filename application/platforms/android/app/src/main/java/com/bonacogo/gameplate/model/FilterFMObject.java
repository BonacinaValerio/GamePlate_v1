package com.bonacogo.gameplate.model;

import android.content.SharedPreferences;

import com.bonacogo.gameplate.util.SharedObject;

public class FilterFMObject {
    public static final String DETAILS_STRING = "FILTER_FM_DETAILS";
    private static final int DISTANCE = 0;
    private static final int RELEVANCE = 1;
    private static final int REWARD = 2;
    private static final int DISTANCE_PRESET = 20;
    private static final int ORDER_BY_PRESET = DISTANCE;

    private int distance, order_by;

    public FilterFMObject() {
        this.distance = DISTANCE_PRESET;
        this.order_by = ORDER_BY_PRESET;
    }

    public FilterFMObject(int distance, int order_by) {
        this.distance = distance;
        this.order_by = order_by;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance, SharedPreferences prefs) {
        // controllo preliminare
        if (distance>=10 && distance <= 50)
            this.distance = distance;
        else
            return;

        SharedObject.saveObject(this, prefs, DETAILS_STRING);
    }

    public int getOrder_by() {
        return order_by;
    }

    public void setOrder_by(int order_by, SharedPreferences prefs) {
        // controllo preliminare
        if (order_by == DISTANCE || order_by == RELEVANCE || order_by == REWARD)
            this.order_by = order_by;
        else
            return;

        SharedObject.saveObject(this, prefs, DETAILS_STRING);
    }
}
