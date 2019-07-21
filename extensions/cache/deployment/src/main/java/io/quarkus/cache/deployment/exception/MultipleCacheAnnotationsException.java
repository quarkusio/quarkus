package io.quarkus.cache.deployment.exception;

@SuppressWarnings("serial")
public class MultipleCacheAnnotationsException extends RuntimeException {

    public MultipleCacheAnnotationsException(String message) {
        super(message);
    }
}
