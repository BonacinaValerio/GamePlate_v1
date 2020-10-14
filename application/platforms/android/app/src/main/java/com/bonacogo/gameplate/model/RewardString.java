package com.bonacogo.gameplate.model;

public class RewardString {
    private String target, terms, description;

    public RewardString() {
    }

    public RewardString(String target, String terms, String description) {
        this.target = target;
        this.terms = terms;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}