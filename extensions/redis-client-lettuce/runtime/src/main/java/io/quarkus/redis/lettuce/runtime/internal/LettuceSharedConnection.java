package io.quarkus.redis.lettuce.runtime.internal;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;

/**
 * The connection the commands of the data sources of a client share, opened by the first command instead of at
 * startup, as the Vert.x data sources open theirs: an application starts while its Redis server is still away, and
 * the commands sent meanwhile fail.
 * <p>
 * Lettuce has no lazy connection, so the commands arriving before the connection is established wait for it here.
 * They are sent in the order they arrived, and a command arriving while they are being sent queues behind them: the
 * connection is only {@link #established()}, for a command to be sent at once, when nobody waits anymore. A failed
 * attempt fails the commands that waited for it and is forgotten, so the next command tries again; an established
 * connection is kept for good, Lettuce reconnecting it when it is lost.
 */
public final class LettuceSharedConnection implements LettuceConnectionSource {

    private final Supplier<CompletionStage<LettuceConnection>> connector;
    private final boolean cluster;
    private final Object lock = new Object();
    /** Set once the waiters of the attempt that established it are served. */
    private volatile LettuceConnection established;
    /** The commands waiting for the attempt in progress, in arrival order; {@code null} when there is none. */
    private List<UniEmitter<? super LettuceConnection>> waiters;
    private boolean closed;

    /**
     * @param connector opens a connection, called by the first command and again by the first one after a failure
     * @param cluster whether {@code connector} connects to a cluster
     */
    public LettuceSharedConnection(Supplier<CompletionStage<LettuceConnection>> connector, boolean cluster) {
        this.connector = nonNull(connector, "connector");
        this.cluster = cluster;
    }

    @Override
    public LettuceConnection established() {
        return established;
    }

    @Override
    public boolean isCluster() {
        return cluster;
    }

    @Override
    public Uni<LettuceConnection> connect() {
        return Uni.createFrom().emitter(emitter -> {
            LettuceConnection connection;
            boolean first = false;
            synchronized (lock) {
                connection = established;
                if (connection == null) {
                    if (closed) {
                        emitter.fail(closedFailure());
                        return;
                    }
                    if (waiters == null) {
                        waiters = new ArrayList<>();
                        first = true;
                    }
                    waiters.add(emitter);
                }
            }
            if (connection != null) {
                emitter.complete(connection);
            } else if (first) {
                open();
            }
        });
    }

    private void open() {
        CompletionStage<LettuceConnection> connecting;
        try {
            connecting = connector.get();
        } catch (RuntimeException e) {
            connecting = CompletableFuture.failedStage(e);
        }
        connecting.whenComplete((connection, failure) -> {
            if (failure != null) {
                fail(failure);
            } else {
                serve(connection);
            }
        });
    }

    /**
     * Hands the connection to the commands that waited for it, in arrival order, the ones arriving meanwhile
     * included, and only then publishes it.
     */
    private void serve(LettuceConnection connection) {
        while (true) {
            List<UniEmitter<? super LettuceConnection>> batch;
            synchronized (lock) {
                if (closed) {
                    break;
                }
                if (waiters.isEmpty()) {
                    waiters = null;
                    established = connection;
                    return;
                }
                batch = waiters;
                waiters = new ArrayList<>();
            }
            for (UniEmitter<? super LettuceConnection> waiter : batch) {
                waiter.complete(connection);
            }
        }
        // closed while connecting
        connection.closeAsync();
        fail(closedFailure());
    }

    private void fail(Throwable completion) {
        // a stage derived from the one Lettuce fails wraps the failure
        Throwable failure = completion instanceof CompletionException && completion.getCause() != null
                ? completion.getCause()
                : completion;
        List<UniEmitter<? super LettuceConnection>> batch;
        synchronized (lock) {
            batch = waiters;
            waiters = null;
        }
        if (batch != null) {
            for (UniEmitter<? super LettuceConnection> waiter : batch) {
                waiter.fail(failure);
            }
        }
    }

    /**
     * Closes the connection, or the one being opened once it is established; the commands waiting for it fail, as
     * every later one does.
     */
    public void close() {
        LettuceConnection connection;
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            connection = established;
            established = null;
        }
        if (connection != null) {
            connection.close();
        }
    }

    private static IllegalStateException closedFailure() {
        return new IllegalStateException("The Redis connection of the data source is closed");
    }

}
