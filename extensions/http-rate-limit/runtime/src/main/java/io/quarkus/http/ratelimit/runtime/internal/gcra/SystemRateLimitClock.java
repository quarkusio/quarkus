package io.quarkus.http.ratelimit.runtime.internal.gcra;

import java.time.Duration;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Reserve;
import jakarta.inject.Singleton;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;

/**
 * {@link System#nanoTime()} and Vert.x timers. A timer set from a Vert.x context runs on that context.
 */
@Singleton
@Reserve
@Priority(0)
public class SystemRateLimitClock implements RateLimitClock {

    private final Vertx vertx;

    public SystemRateLimitClock(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public long nanoTime() {
        return System.nanoTime();
    }

    @Override
    public Cancellable schedule(Duration delay, Runnable task) {
        // Vert.x timers have millisecond resolution and a minimum of 1 ms: round up so that a task never runs early
        long delayMillis = Math.max(1, delay.plusNanos(999_999).toMillis());
        long id = vertx.setTimer(delayMillis, new Handler<>() {
            @Override
            public void handle(Long ignored) {
                task.run();
            }
        });
        return new Cancellable() {
            @Override
            public void cancel() {
                vertx.cancelTimer(id);
            }
        };
    }
}
