package io.quarkus.reactive.transaction.runtime.pool;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import org.jboss.logging.Logger;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.sqlclient.Pool;

/**
 * Holder for the connection future, ensuring only one connection is created per transaction
 * even when multiple sessions request it concurrently.
 */
class ConnectionHolder {

    private static final Logger LOG = Logger.getLogger(ConnectionHolder.class);

    private static final VarHandle OPENED_HANDLE;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            OPENED_HANDLE = lookup.findVarHandle(ConnectionHolder.class, "opened", boolean.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final Pool delegate;
    final Promise<TransactionalContextConnection> connectionPromise = Promise.promise();
    private volatile boolean opened = false;

    ConnectionHolder(Pool delegate) {
        this.delegate = delegate;
    }

    /**
     * Gets the connection, creating it if this is the first call (thread-safe).
     * Concurrent calls will wait on the same future.
     */
    Future<TransactionalContextConnection> getConnection() {
        if (opened) {
            return connectionPromise.future();
        }
        // Use compareAndSet to ensure only one thread initiates the connection
        if (OPENED_HANDLE.compareAndSet(this, false, true)) {
            delegate.getConnection()
                    .compose(connection -> {
                        LOG.tracef("New connection, about to start transaction: %s", connection);
                        return connection.begin().map(t -> {
                            LOG.tracef("Transaction started: %s", connection);
                            return new TransactionalContextConnection(connection);
                        });
                    })
                    .onComplete(connectionPromise);
        }
        return connectionPromise.future();
    }
}
