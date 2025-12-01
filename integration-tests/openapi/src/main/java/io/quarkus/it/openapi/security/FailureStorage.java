package io.quarkus.it.openapi.security;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.logging.Log;

@ApplicationScoped
public class FailureStorage {

    private volatile Throwable throwable = null;

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        Log.info("Setting throwable value to " + throwable);
        this.throwable = throwable;
    }
}
