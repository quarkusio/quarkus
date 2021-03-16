package io.quarkus.reactivemessaging.utils;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.Vertx;

public class VertxFriendlyLock {
    private final Vertx vertx;

    volatile boolean ready = true;
    private final List<Long> timers = new ArrayList<>();

    public VertxFriendlyLock(Vertx vertx) {
        this.vertx = vertx;
    }

    public void triggerWhenUnlocked(Runnable action, long maxTime) {
        triggerWhenUnlocked(action, System.currentTimeMillis(), maxTime);
    }

    private void triggerWhenUnlocked(Runnable action, long startTime, long maxTime) {
        if (System.currentTimeMillis() - startTime >= maxTime) {
            fail("the consumer not released in " + maxTime + " ms");
        }
        if (!ready) {
            timers.add(vertx.setTimer(100, timer -> triggerWhenUnlocked(action, startTime, maxTime)));
        } else {
            action.run();
        }
    }

    public void lock() {
        ready = false;
    }

    public void unlock() {
        ready = true;
    }

    public void reset() {
        for (Long timer : timers) {
            vertx.cancelTimer(timer);
        }
        ready = true;
    }
}
