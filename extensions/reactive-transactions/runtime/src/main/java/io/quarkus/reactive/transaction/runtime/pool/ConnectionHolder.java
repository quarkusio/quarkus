package io.quarkus.reactive.transaction.runtime.pool;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import org.jboss.logging.Logger;

import io.quarkus.reactive.transaction.runtime.ReactiveTransactionResource;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.sqlclient.Pool;

/**
 * Holder for the connection future, ensuring only one connection is created per transaction
 * even when multiple sessions request it concurrently.
 * <p>
 * Implements {@link ReactiveTransactionResource} to participate in the reactive transaction lifecycle.
 */
public class ConnectionHolder implements ReactiveTransactionResource {

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

    public ConnectionHolder(Pool delegate) {
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

    @Override
    public Future<Void> commit() {
        return connectionPromise.future()
                .compose(wrappedConnection -> {
                    if (wrappedConnection.transaction() == null) {
                        LOG.tracef("Transaction doesn't exist, so won't commit here");
                        return Future.succeededFuture();
                    }
                    return wrappedConnection.transaction().commit()
                            .onFailure(t -> LOG.tracef("Failed to commit transaction"))
                            .onSuccess(v -> LOG.tracef("Transaction committed"));
                });
    }

    @Override
    public Future<Void> rollback() {
        return connectionPromise.future()
                .compose(wrappedConnection -> {
                    if (wrappedConnection.transaction() == null) {
                        LOG.tracef("Transaction doesn't exist, so won't rollback here");
                        return Future.succeededFuture();
                    }
                    return wrappedConnection.transaction().rollback()
                            .onFailure(t -> LOG.tracef("Failed to rollback transaction"))
                            .onSuccess(v -> LOG.tracef("Transaction rolled back"));
                });
    }

    @Override
    public Future<Void> close() {
        return connectionPromise.future()
                .compose(wrappedConnection -> {
                    LOG.tracef("Closing the underlying connection");
                    return wrappedConnection.getDelegate().close();
                });
    }
}
