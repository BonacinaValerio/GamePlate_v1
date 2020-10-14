package com.bonacogo.gameplate.model;

import java.io.Serializable;

public class RewardObject implements Serializable {
    private MyDate endAt, startAt;
    private String restaurantId, type, url, description, target, terms, extra;

    public RewardObject(MyDate endAt, MyDate startAt, String restaurantId, String type, String url, String description, String target, String terms, String extra) {
        this.endAt = endAt;
        this.startAt = startAt;
        this.restaurantId = restaurantId;
        this.type = type;
        this.url = url;
        this.description = description;
        this.target = target;
        this.terms = terms;
        this.extra = extra;
    }

    public RewardObject() {
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public MyDate getEndAt() {
        return endAt;
    }

    public void setEndAt(MyDate endAt) {
        this.endAt = endAt;
    }

    public MyDate getStartAt() {
        return startAt;
    }

    public void setStartAt(MyDate startAt) {
        this.startAt = startAt;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getTerms() {
        return terms;
    }

    public void setTerms(String terms) {
        this.terms = terms;
    }
}

