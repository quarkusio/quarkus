package io.quarkus.cache.deployment.exception;

@SuppressWarnings("serial")
public class IllegalReturnTypeException extends RuntimeException {

    public IllegalReturnTypeException(String message) {
        super(message);
    }
}
