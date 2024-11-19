package io.quarkus.websockets.next.runtime.telemetry;

import java.util.Map;

import io.micrometer.core.instrument.Counter;

final class MetricsConnectionInterceptor implements ConnectionInterceptor {

    private final Counter connectionOpenCounter;
    private final Counter connectionOpeninigFailedCounter;

    MetricsConnectionInterceptor(Counter connectionOpenCounter, Counter connectionOpeninigFailedCounter) {
        this.connectionOpenCounter = connectionOpenCounter;
        this.connectionOpeninigFailedCounter = connectionOpeninigFailedCounter;
    }

    @Override
    public void connectionOpened() {
        connectionOpenCounter.increment();
    }

    @Override
    public void connectionOpeningFailed(Throwable cause) {
        connectionOpeninigFailedCounter.increment();
    }

    @Override
    public Map<String, Object> getContextData() {
        return Map.of();
    }
}
