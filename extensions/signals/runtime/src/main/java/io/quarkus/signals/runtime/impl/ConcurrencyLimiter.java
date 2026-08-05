package io.quarkus.signals.runtime.impl;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

class ConcurrencyLimiter {

    private static final Logger LOG = Logger.getLogger(ConcurrencyLimiter.class);

    private final int limit;
    private final AtomicInteger running;
    private final Queue<Action> queue;

    ConcurrencyLimiter(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException(
                    "Limit must be a positive integer: " + limit);
        }
        this.limit = limit;
        this.running = new AtomicInteger();
        this.queue = new ConcurrentLinkedQueue<>();
    }

    void run(Runnable task, Consumer<Throwable> onError) {
        queue.offer(new Action(task, onError));
        LOG.debugf("Queue action [running=%s, limit=%s]", running, limit);
        tryDrain();
    }

    /**
     * Signals that a previously started action has finished. Must be called exactly once per started action, and must not be
     * called synchronously inside the action's {@link Runnable#run()} — doing so causes recursive {@link #tryDrain()} calls
     * whose stack depth equals the queue size.
     */
    void complete() {
        int current = running.get();
        while (current > 0) {
            if (running.compareAndSet(current, current - 1)) {
                tryDrain();
                return;
            }
            current = running.get();
        }
    }

    private void tryDrain() {
        int current = running.get();
        while (current < limit) {
            if (running.compareAndSet(current, current + 1)) {
                Action action = queue.poll();
                if (action != null) {
                    try {
                        LOG.debugf("Run queued action [running=%s, limit=%s]", current + 1, limit);
                        action.task.run();
                    } catch (Throwable t) {
                        running.decrementAndGet();
                        try {
                            action.onError.accept(t);
                        } catch (Throwable handlerError) {
                            LOG.errorf(handlerError, "Error handler failed");
                        }
                    }
                } else {
                    running.decrementAndGet();
                    if (queue.isEmpty()) {
                        return;
                    }
                }
            }
            current = running.get();
        }
    }

    private record Action(Runnable task, Consumer<Throwable> onError) {
    }
}
