package io.quarkus.spring.web.test.basic;

public class Greeting {

    private final String message;

    public Greeting(final String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
