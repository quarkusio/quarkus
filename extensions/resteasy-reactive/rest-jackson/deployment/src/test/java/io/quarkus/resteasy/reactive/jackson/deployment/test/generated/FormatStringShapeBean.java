package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import com.fasterxml.jackson.annotation.JsonFormat;

public class FormatStringShapeBean {

    private String name;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private int count;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private double score;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private boolean active;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private long bigNumber;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getBigNumber() {
        return bigNumber;
    }

    public void setBigNumber(long bigNumber) {
        this.bigNumber = bigNumber;
    }
}
