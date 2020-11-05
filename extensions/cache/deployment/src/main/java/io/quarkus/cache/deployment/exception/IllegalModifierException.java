package io.quarkus.cache.deployment.exception;

@SuppressWarnings("serial")
public class IllegalModifierException extends RuntimeException {

    public IllegalModifierException(String message) {
        super(message);
    }
}
