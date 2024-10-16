package io.quarkus.resteasy.reactive.jackson.deployment.test;

public class FroMage {
    public Integer price;

    public FroMage(Integer price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "FroMage: " + price;
    }
}
