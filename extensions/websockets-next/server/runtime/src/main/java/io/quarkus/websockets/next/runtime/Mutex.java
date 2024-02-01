package io.quarkus.websockets.next.runtime;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.quarkus.websockets.next.WebSocketServerConnection;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Promise;

/**
 * TODO implement a more robust solution to ensure that endpoint callbacks are not invoked concurrently.
 */
class Mutex {

    private static final Logger LOG = Logger.getLogger(Mutex.class);

    private final Context context;
    private final WebSocketServerConnection connection;

    private volatile boolean acquired;
    private final Lock lock = new ReentrantLock();
    private final ConcurrentLinkedQueue<Runnable> queue;

    Mutex(Context context, WebSocketServerConnection connection) {
        this.context = context;
        this.connection = connection;
        this.queue = new ConcurrentLinkedQueue<>();
    }

    Consumer<Void> promiseComplete(Promise<Void> promise) {
        return new PromiseComplete(promise);
    }

    void run(Runnable action) {
        boolean run = false;
        lock.lock();
        try {
            if (!acquired) {
                acquired = true;
                run = true;
            }
        } finally {
            lock.unlock();
        }
        if (run) {
            LOG.debugf("Permit acquired: %s", connection);
            action.run();
        } else {
            LOG.debugf("Action queued: %s", connection);
            queue.add(action);
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
                Runnable action;
                lock.lock();
                try {
                    action = queue.poll();
                    if (action == null) {
                        acquired = false;
                    }
                } finally {
                    lock.unlock();
                }
                if (action == null) {
                    LOG.debugf("Permit released: %s", connection);
                } else {
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
