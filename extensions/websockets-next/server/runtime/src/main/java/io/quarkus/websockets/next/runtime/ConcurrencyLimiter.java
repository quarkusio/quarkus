package io.quarkus.websockets.next.runtime;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.quarkus.websockets.next.WebSocketServerConnection;
import io.smallrye.mutiny.helpers.queues.Queues;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Promise;

/**
 * Used to limit concurrent invocations. Endpoint callbacks may not be invoked concurrently.
 */
class ConcurrencyLimiter {

    private static final Logger LOG = Logger.getLogger(ConcurrencyLimiter.class);

    private final Context context;
    private final WebSocketServerConnection connection;

    private final Queue<Runnable> queue;
    private final AtomicLong uncompleted;

    ConcurrencyLimiter(Context context, WebSocketServerConnection connection) {
        this.context = context;
        this.connection = connection;
        this.uncompleted = new AtomicLong();
        this.queue = Queues.createMpscQueue();
    }

    /**
     * This method must be always used before {@link #run(Runnable)} and the returned callback must be always invoked when an
     * async computation completes.
     *
     * @param promise
     * @return a new callback to complete the given promise
     */
    Consumer<Void> newComplete(Promise<Void> promise) {
        return new PromiseComplete(promise);
    }

    /**
     * Run or queue up the given action.
     *
     * @param action
     */
    void run(Runnable action) {
        if (uncompleted.compareAndSet(0, 1)) {
            LOG.debugf("Run action: %s", connection);
            action.run();
        } else {
            LOG.debugf("Action queued: %s", connection);
            queue.offer(action);
            // We need to make sure that at least one completion is in flight
            if (uncompleted.getAndIncrement() == 0) {
                LOG.debugf("Run action: %s", connection);
                Runnable runnable = queue.poll();
                runnable.run();
            }
        }
    }

    class PromiseComplete implements Consumer<Void> {

        final Promise<Void> promise;

        private PromiseComplete(Promise<Void> promise) {
            this.promise = promise;
        }

        @Override
        public void accept(Void t) {
            try {
                promise.complete();
            } finally {
                if (uncompleted.decrementAndGet() == 0) {
                    return;
                }
                Runnable action = queue.poll();
                if (action != null) {
                    LOG.debugf("Run action from queue: %s", connection);
                    context.runOnContext(new Handler<Void>() {
                        @Override
                        public void handle(Void event) {
                            action.run();
                        }
                    });
                }
            }
        }

    }
}
