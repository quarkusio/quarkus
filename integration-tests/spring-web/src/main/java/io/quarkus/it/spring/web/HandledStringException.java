package io.quarkus.it.spring.web;

public class HandledStringException extends RuntimeException {

    public HandledStringException(String message) {
        super(message);
    }
}
