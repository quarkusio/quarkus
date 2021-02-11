package io.quarkus.mongodb.metrics;

import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.microprofile.metrics.Gauge;

public class ConnectionPoolGauge implements Gauge<Long> {
    private AtomicLong value = new AtomicLong();

    public void decrement() {
        value.decrementAndGet();
    }

    public void increment() {
        value.incrementAndGet();
    }

    @Override
    public Long getValue() {
        return value.longValue();
    }
}
