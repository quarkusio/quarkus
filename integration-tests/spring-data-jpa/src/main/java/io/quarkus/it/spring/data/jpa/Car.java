package io.quarkus.it.spring.data.jpa;

import jakarta.persistence.Entity;

@Entity(name = "MotorCar")
public class Car extends AbstractEntity {

    private String brand;
    private String model;

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
