package org.acme.common.domain;

public class CarDTO {
    private String type;
    private Integer seatNumber;

    public CarDTO() {
    }

    public CarDTO(String type, Integer seatNumber) {
        this.type = type;
        this.seatNumber = seatNumber;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }
}