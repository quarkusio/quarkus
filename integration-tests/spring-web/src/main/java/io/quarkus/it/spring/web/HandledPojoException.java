package io.quarkus.it.spring.web;

public class HandledPojoException extends RuntimeException {

    public HandledPojoException(String message) {
        super(message);
    }
}
