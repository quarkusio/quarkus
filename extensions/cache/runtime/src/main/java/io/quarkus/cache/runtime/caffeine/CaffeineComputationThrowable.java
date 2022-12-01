package io.quarkus.cache.runtime.caffeine;

/**
 * This class is used to prevent Caffeine from logging unwanted warnings.
 */
public class CaffeineComputationThrowable {

    private Throwable cause;

    public CaffeineComputationThrowable(Throwable cause) {
        this.cause = cause;
    }

    public Throwable getCause() {
        return cause;
    }
}
