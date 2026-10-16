package io.quarkus.core;

public interface AsyncStopContext {
    /**
     * Indicate that the service has stopped.
     * This method is idempotent (calling it again will have no additional effect).
     */
    void stopComplete();
}
