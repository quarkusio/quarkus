package io.quarkus.smallrye.faulttolerance.test.fallback.causechain;

public class ExpectedOutcomeException extends Exception {
    public ExpectedOutcomeException() {
    }

    public ExpectedOutcomeException(Throwable cause) {
        super(cause);
    }
}
