package io.quarkus.resteasy.reactive.jackson.deployment.test;

public class Score {

    private String category;
    private int value;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
