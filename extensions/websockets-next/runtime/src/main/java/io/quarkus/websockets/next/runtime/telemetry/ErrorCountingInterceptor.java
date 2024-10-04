package io.quarkus.websockets.next.runtime.telemetry;

import io.micrometer.core.instrument.Counter;

final class ErrorCountingInterceptor implements ErrorInterceptor {

    private final Counter errorCounter;

    ErrorCountingInterceptor(Counter errorCounter) {
        this.errorCounter = errorCounter;
    }

    @Override
    public void intercept(Throwable throwable) {
        errorCounter.increment();
    }
}
