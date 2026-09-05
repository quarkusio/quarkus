package io.quarkus.redis.runtime.datasource;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.RedisConnection;

/**
 * Utility to borrow a connection from Redis pool and reliably release it, even when the subscription
 * is cancelled while the connection is still being acquired.
 */
public final class RedisConnections {
    /**
     * Acquires a dedicated connection from the pool, applies the given {@code function} to it and releases the
     * connection back to the pool once the produced {@link Uni} terminates (completion, failure or cancellation).
     * <p>
     * The Vert.x {@code redis.connect()} operation is backed by a {@code Future} that cannot be cancelled, so an
     * in-flight acquisition cannot be aborted. The pool will hand over a connection even if the caller has already
     * cancelled. To avoid leaking the connection, we keep the acquisition subscribed rather than cancelling it,
     * and release any connection that is delivered after cancellation. Do not "optimize" this by cancelling the
     * acquisition; doing so detaches the handler and never returns the acquired connection to the pool.
     */
    public static <T> Uni<T> withNewConnection(Redis redis, Function<RedisConnection, Uni<T>> function) {
        return withNewConnection(redis, function, true);
    }

    /**
     * Like {@link #withNewConnection(Redis, Function)}, but the connection is released only when the produced
     * {@link Uni} fails or is cancelled. On success, the connection is kept open and its lifecycle becomes the
     * caller's responsibility. This suits long-lived connections such as a pub/sub subscription that is closed
     * when the caller unsubscribes.
     */
    public static <T> Uni<T> withNewLongLivedConnection(Redis redis,
            Function<RedisConnection, Uni<T>> function) {
        return withNewConnection(redis, function, false);
    }

    private static <T> Uni<T> withNewConnection(Redis redis, Function<RedisConnection, Uni<T>> function,
            boolean releaseOnSuccess) {
        return Uni.createFrom().emitter(emitter -> {
            ConnectionTracker<T> tracker = new ConnectionTracker<>(emitter, function, releaseOnSuccess);
            emitter.onTermination(tracker::cancel);
            redis.connect().subscribe().with(emitter.context(), tracker::connected, tracker::fail);
        });
    }

    /**
     * Tracks the lifecycle of a single connection acquisition in a lock-free manner.
     * <p>
     * The {@link #state} holds one of three things:
     * <ul>
     * <li>{@link #INITIAL} while the connection is being acquired</li>
     * <li>the {@link Cancellable} of the running operation once the connection has been delivered</li>
     * <li>{@link #DONE} once the execution has completed, failed or been cancelled</li>
     * </ul>
     */
    private static final class ConnectionTracker<T> {
        private static final Object INITIAL = new Object();
        private static final Object DONE = new Object();

        private final UniEmitter<? super T> emitter;
        private final Function<RedisConnection, Uni<T>> function;
        private final boolean releaseOnSuccess;
        private final AtomicReference<Object> state = new AtomicReference<>(INITIAL);

        private ConnectionTracker(UniEmitter<? super T> emitter, Function<RedisConnection, Uni<T>> function,
                boolean releaseOnSuccess) {
            this.emitter = emitter;
            this.function = function;
            this.releaseOnSuccess = releaseOnSuccess;
        }

        private void connected(RedisConnection connection) {
            if (state.get() == DONE) {
                connection.closeAndForget();
                return;
            }

            Uni<T> result;
            try {
                result = nonNull(function.apply(connection), "The function must not return null");
            } catch (Throwable failure) {
                result = Uni.createFrom().<T> failure(failure);
            }

            Uni<T> guarded = releaseOnSuccess
                    ? result.onTermination().call(connection::close)
                    : result.onFailure().call(connection::close).onCancellation().call(connection::close);

            Cancellable action = guarded.subscribe().with(emitter.context(), this::complete, this::fail);

            if (!state.compareAndSet(INITIAL, action)) {
                action.cancel();
            }
        }

        private void complete(T item) {
            if (state.getAndSet(DONE) != DONE) {
                emitter.complete(item);
            }
        }

        private void fail(Throwable failure) {
            if (state.getAndSet(DONE) != DONE) {
                emitter.fail(failure);
            }
        }

        private void cancel() {
            Object previous = state.getAndSet(DONE);
            if (previous != INITIAL && previous != DONE) {
                ((Cancellable) previous).cancel();
            }
        }
    }
}
