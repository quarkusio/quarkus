package io.quarkus.spring.web.resteasy.reactive.test;

public class Greeting {

    private final String message;

    public Greeting(final String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
