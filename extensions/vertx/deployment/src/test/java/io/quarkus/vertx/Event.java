package io.quarkus.vertx;

public class Event {

    private String property;

    public Event(String property) {
        this.property = property;
    }

    public String getProperty() {
        return property;
    }
}
