package io.quarkus.redis.lettuce.runtime.internal;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * Utility for adapting Lettuce {@link CompletionStage} results to Mutiny {@link Uni}s that emit on the caller's
 * Vert.x context, and to blocking calls.
 * <p>
 * Lettuce async commands return {@link io.lettuce.core.RedisFuture} which extends {@link CompletionStage}.
 */
public final class LettuceResult {

    private LettuceResult() {
        // Utility class
    }

    /**
     * Converts a Lettuce {@link CompletionStage} into a {@link Uni} that emits on the Vert.x context of the caller.
     * <p>
     * Lettuce completes a command on the event loop of its connection, which is rarely the one serving the caller,
     * whereas the Vert.x Redis client completes on the caller's context. So the Vert.x context current at
     * subscription is captured and, when it is an event loop context (the duplicated context of the request being
     * served included), the item or failure is re-dispatched to it: the continuation runs where the caller runs and
     * finds the context locals of the request. Without a context, or on a worker context, the continuation runs
     * where Lettuce completed the command: a worker context runs its tasks in order, so dispatching to it from a
     * task that may be blocked on the result would deadlock, and a blocking caller does not need the context.
     * {@code supplier} is invoked at subscription, so the command is sent lazily.
     *
     * @param <T> the result type
     * @param supplier the Lettuce call, typically an asynchronous command
     * @return a {@link Uni} completing with the result of the {@link CompletionStage}, on the caller's context
     */
    public static <T> Uni<T> toUni(Supplier<? extends CompletionStage<T>> supplier) {
        return onCallerContext(() -> Uni.createFrom().completionStage(supplier));
    }

    /**
     * Like {@link #toUni(Supplier)}, for a call needing the connection of a {@link LettuceConnectionSource}: sent at
     * once on an established connection, and once a shared connection that is still to be opened is established
     * otherwise, failing when it cannot be.
     *
     * @param <T> the result type
     * @param source where the connection comes from
     * @param call the Lettuce call on that connection, typically an asynchronous command
     * @return a {@link Uni} completing with the result of the {@link CompletionStage}, on the caller's context
     */
    public static <T> Uni<T> toUni(LettuceConnectionSource source,
            Function<? super LettuceConnection, ? extends CompletionStage<T>> call) {
        return onCallerContext(() -> {
            LettuceConnection established = source.established();
            if (established != null) {
                return Uni.createFrom().completionStage(() -> call.apply(established));
            }
            return source.connect()
                    .chain(connection -> Uni.createFrom().completionStage(() -> call.apply(connection)));
        });
    }

    private static <T> Uni<T> onCallerContext(Supplier<Uni<T>> source) {
        return Uni.createFrom().deferred(() -> {
            Context context = Vertx.currentContext();
            Uni<T> uni = source.get();
            if (context == null || !context.isEventLoopContext()) {
                return uni;
            }
            return uni.emitOn(task -> {
                if (Vertx.currentContext() == context) {
                    task.run();
                } else {
                    context.runOnContext(ignored -> task.run());
                }
            });
        });
    }

    /**
     * Blocks on a {@link CompletionStage} and returns the result.
     * <p>
     * Must only be called from a worker thread, never from an event loop thread; this is enforced
     * via {@link Context#isOnEventLoopThread()}.
     *
     * @param <T> the result type
     * @param stage the {@link CompletionStage} to block on
     * @param timeout the maximum time to wait
     * @return the result
     * @throws IllegalStateException if invoked on an event loop thread
     * @throws java.util.concurrent.CompletionException if the computation threw an exception
     */
    public static <T> T toBlocking(CompletionStage<T> stage, Duration timeout) {
        if (Context.isOnEventLoopThread()) {
            throw new IllegalStateException(
                    "LettuceResult.toBlocking must not be called from an event loop thread");
        }
        return Uni.createFrom().completionStage(stage)
                .await().atMost(timeout);
    }
}
