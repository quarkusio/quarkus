package io.quarkus.it.shared;

public class Shared {

    private final String message;

    public Shared(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
