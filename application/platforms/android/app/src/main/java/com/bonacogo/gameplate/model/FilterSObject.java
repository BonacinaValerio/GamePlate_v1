package com.bonacogo.gameplate.model;

import android.content.SharedPreferences;

import com.bonacogo.gameplate.util.SharedObject;

import java.io.Serializable;

public class FilterSObject implements Serializable {
    public static final String DETAILS_STRING = "FILTER_S_DETAILS";
    private static final int RELEVANCE = 0;
    private static final int REWARD = 1;
    private static final int ORDER_BY_PRESET = RELEVANCE;

    private int order_by;

    public FilterSObject() {
        this.order_by = ORDER_BY_PRESET;
    }

    public FilterSObject(int order_by) {
        this.order_by = order_by;
    }

    public int getOrder_by() {
        return order_by;
    }

    public void setOrder_by(int order_by, SharedPreferences prefs) {
        // controllo preliminare
        if (order_by == RELEVANCE || order_by == REWARD)
            this.order_by = order_by;
        else
            return;

        SharedObject.saveObject(this, prefs, DETAILS_STRING);
    }


}
