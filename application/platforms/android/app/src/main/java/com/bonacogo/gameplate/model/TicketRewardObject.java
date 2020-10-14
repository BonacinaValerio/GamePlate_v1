package com.bonacogo.gameplate.model;

import java.io.Serializable;

public class TicketRewardObject implements Serializable {
    private long deadline, startAt;
    private boolean enable;
    private String gameId, RestaurantId, background, game, idReward, restaurant, ticketCode, type;
    private RewardString rewardString;

    public TicketRewardObject() {
    }

    public TicketRewardObject(long deadline, long startAt, boolean enable, String gameId, String restaurantId, String background, String game, String idReward, String restaurant, String ticketCode, String type, RewardString rewardString) {
        this.deadline = deadline;
        this.startAt = startAt;
        this.enable = enable;
        this.gameId = gameId;
        RestaurantId = restaurantId;
        this.background = background;
        this.game = game;
        this.idReward = idReward;
        this.restaurant = restaurant;
        this.ticketCode = ticketCode;
        this.type = type;
        this.rewardString = rewardString;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getRestaurantId() {
        return RestaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        RestaurantId = restaurantId;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public RewardString getRewardString() {
        return rewardString;
    }

    public void setRewardString(RewardString rewardString) {
        this.rewardString = rewardString;
    }

    public long getDeadline() {
        return deadline;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }

    public long getStartAt() {
        return startAt;
    }

    public void setStartAt(long startAt) {
        this.startAt = startAt;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getGame() {
        return game;
    }

    public void setGame(String game) {
        this.game = game;
    }

    public String getIdReward() {
        return idReward;
    }

    public void setIdReward(String idReward) {
        this.idReward = idReward;
    }

    public String getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(String restaurant) {
        this.restaurant = restaurant;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
