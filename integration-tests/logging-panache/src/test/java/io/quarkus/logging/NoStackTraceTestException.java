package io.quarkus.logging;

public class NoStackTraceTestException extends RuntimeException {
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
