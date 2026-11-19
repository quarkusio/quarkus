package io.quarkus.http.ratelimit.runtime.internal.gcra;

import java.time.Duration;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * A {@link RateLimitClock} for tests: time moves only on {@link #advance(Duration)}, which then runs every scheduled
 * task whose due time has passed, in due-time order (ties in scheduling order). Tasks run on the calling thread.
 */
public class MutableClock implements RateLimitClock {

    private record Scheduled(long due, long seq, Runnable task) {
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
        Scheduled scheduled = new Scheduled(now + delay.toNanos(), seq++, task);
        tasks.add(scheduled);
        return () -> {
            synchronized (MutableClock.this) {
                tasks.remove(scheduled);
            }
        };
    }

    /**
     * @return the number of tasks scheduled so far, cancelled ones included
     */
    public synchronized long scheduledCount() {
        return seq;
    }

    /**
     * Moves time forward by {@code duration}, then runs the tasks that are due, including those that due tasks
     * schedule.
     */
    public void advance(Duration duration) {
        synchronized (this) {
            now += duration.toNanos();
        }
        Runnable next;
        while ((next = pollDue()) != null) {
            next.run();
        }
    }

    private synchronized Runnable pollDue() {
        Scheduled head = tasks.peek();
        if (head == null || head.due - now > 0) {
            return null;
        }
        return tasks.poll().task;
    }
}
