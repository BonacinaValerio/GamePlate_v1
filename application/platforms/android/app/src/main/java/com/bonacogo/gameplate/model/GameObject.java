package com.bonacogo.gameplate.model;

import java.io.Serializable;
import java.util.ArrayList;

public class GameObject implements Serializable {
    private String game, restaurant, address, numFeedback, url, description, web, id;
    private String[] type;
    private float rating;
    private Double lat, lng;
    private ArrayList<RewardObject> rewards;

    public GameObject(String game, String restaurant, String address, String description, String web, String[] type, String id, float rating, String numFeedback, String url, Double lat, Double lng, ArrayList<RewardObject> rewards) {
        this.game = game;
        this.restaurant = restaurant;
        this.description = description;
        this.address = address;
        this.web = web;
        this.type = type;
        this.id = id;
        this.rating = rating;
        this.numFeedback = numFeedback;
        this.url = url;
        this.lat = lat;
        this.lng = lng;
        this.rewards = rewards;
    }

    public GameObject(String game, String restaurant, String url, String id, Double lat, Double lng) {
        this.game = game;
        this.restaurant = restaurant;
        this.url = url;
        this.id = id;
        this.lat = lat;
        this.lng = lng;
    }

    public ArrayList<RewardObject> getRewards() {
        return rewards;
    }

    public void setRewards(ArrayList<RewardObject> rewards) {
        this.rewards = rewards;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWeb() {
        return web;
    }

    public void setWeb(String web) {
        this.web = web;
    }

    public String[] getType() {
        return type;
    }

    public void setType(String[] type) {
        this.type = type;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public String getGame() {
        return game;
    }

    public void setGame(String game) {
        this.game = game;
    }

    public String getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(String restaurant) {
        this.restaurant = restaurant;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public String getNumFeedback() {
        return numFeedback;
    }

    public void setNumFeedback(String numFeedback) {
        this.numFeedback = numFeedback;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
