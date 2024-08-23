package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Timer;

public class EventTiming {
    private final long nanoStart;
    private final Timer timer;

    public EventTiming(Timer timer) {
        this.timer = timer;
        this.nanoStart = System.nanoTime();
    }

    public long end() {
        long amount = System.nanoTime() - nanoStart;
        if (timer != null) {
            timer.record(amount, TimeUnit.NANOSECONDS);
        }
        return amount;
    }
}
