package io.quarkus.websockets.next.runtime;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.helpers.queues.Queues;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Promise;

/**
 * Used to limit concurrent invocations.
 */
class ConcurrencyLimiter {

    private static final Logger LOG = Logger.getLogger(ConcurrencyLimiter.class);

    private final WebSocketConnectionBase connection;
    private final Queue<Action> queue;
    private final AtomicLong uncompleted;
    private final AtomicLong queueCounter;

    ConcurrencyLimiter(WebSocketConnectionBase connection) {
        this.connection = connection;
        this.uncompleted = new AtomicLong();
        // Counter is only used for debugging
        this.queueCounter = LOG.isDebugEnabled() ? new AtomicLong() : null;
        this.queue = Queues.createMpscQueue();
    }

    /**
     * This method must be always used before {@link #run(Runnable)} and the returned callback must be always invoked when an
     * async computation completes.
     *
     * @param promise
     * @return a new callback to complete the given promise
     */
    PromiseComplete newComplete(Promise<Void> promise) {
        return new PromiseComplete(promise);
    }

    /**
     * Run or queue up the given action.
     *
     * @param action
     * @param context
     */
    void run(Context context, Runnable action) {
        if (uncompleted.compareAndSet(0, 1)) {
            LOG.debugf("Run action: %s", connection);
            action.run();
        } else {
            long queueIndex = queueCounter != null ? queueCounter.incrementAndGet() : 0l;
            LOG.debugf("Action queued as %s: %s", queueIndex, connection);
            queue.offer(new Action(queueIndex, action, context));
            // We need to make sure that at least one completion is in flight
            if (uncompleted.getAndIncrement() == 0) {
                Action queuedAction = queue.poll();
                assert queuedAction != null;
                LOG.debugf("Run action %s from queue: %s", queuedAction.queueIndex, connection);
                queuedAction.runnable.run();
            }
        }
    }

    class PromiseComplete {

        final Promise<Void> promise;

        private PromiseComplete(Promise<Void> promise) {
            this.promise = promise;
        }

        void failure(Throwable t) {
            try {
                promise.fail(t);
            } finally {
                tryNext();
            }
        }

        void complete() {
            try {
                promise.complete();
            } finally {
                tryNext();
            }
        }

        private void tryNext() {
            if (uncompleted.decrementAndGet() == 0) {
                return;
            }
            Action queuedAction = queue.poll();
            assert queuedAction != null;
            LOG.debugf("Run action %s from queue: %s", queuedAction.queueIndex, connection);
            queuedAction.context.runOnContext(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    queuedAction.runnable.run();
                }
            });
        }
    }

    record Action(long queueIndex, Runnable runnable, Context context) {
    }
}
