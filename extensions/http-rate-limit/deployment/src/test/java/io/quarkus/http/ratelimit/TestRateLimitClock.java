package io.quarkus.http.ratelimit;

import java.time.Duration;
import java.util.Comparator;
import java.util.PriorityQueue;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;

import io.quarkus.http.ratelimit.runtime.internal.gcra.RateLimitClock;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * A {@link RateLimitClock} for tests: time moves only on {@link #advance(Duration)}, which then runs every scheduled
 * task whose due time has passed, in due-time order (ties in scheduling order), on the Vert.x context it was scheduled
 * from (on the calling thread if there was none). Time never moves backwards.
 */
@Alternative
@Priority(1)
@Singleton
public class TestRateLimitClock implements RateLimitClock {

    private record Scheduled(long due, long seq, Context context, Runnable task) {
    }

    private final PriorityQueue<Scheduled> tasks = new PriorityQueue<>(
            Comparator.<Scheduled> comparingLong(s -> s.due).thenComparingLong(s -> s.seq));
    private long now;
    private long seq;

    @Override
    public synchronized long nanoTime() {
        return now;
    }

    @Override
    public synchronized Cancellable schedule(Duration delay, Runnable task) {
        Scheduled scheduled = new Scheduled(now + delay.toNanos(), seq++, Vertx.currentContext(), task);
        tasks.add(scheduled);
        return () -> {
            synchronized (TestRateLimitClock.this) {
                tasks.remove(scheduled);
            }
        };
    }

    /**
     * Moves time forward by {@code duration}, then runs the tasks that are due.
     */
    public void advance(Duration duration) {
        if (duration.isNegative()) {
            throw new IllegalArgumentException("The clock never moves backwards");
        }
        synchronized (this) {
            now += duration.toNanos();
        }
        Scheduled next;
        while ((next = pollDue()) != null) {
            Runnable task = next.task;
            if (next.context == null) {
                task.run();
            } else {
                next.context.runOnContext(ignored -> task.run());
            }
        }
    }

    private synchronized Scheduled pollDue() {
        Scheduled head = tasks.peek();
        if (head == null || head.due - now > 0) {
            return null;
        }
        return tasks.poll();
    }
}
