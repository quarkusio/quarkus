package io.quarkus.spring.web.test;

public class SomeClass {

    private String message;

    public SomeClass() {
    }

    public SomeClass(final String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
