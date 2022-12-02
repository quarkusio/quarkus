package org.acme.quarkus.domain;

public class Car {
    private String type;
    private Integer numberOfSeats;

    public Car() {
    }

    public Car(String type, Integer numberOfSeats) {
        this.type = type;
        this.numberOfSeats = numberOfSeats;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getNumberOfSeats() {
        return numberOfSeats;
    }

    public void setNumberOfSeats(Integer numberOfSeats) {
        this.numberOfSeats = numberOfSeats;
    }
}