package io.quarkus.redis.lettuce.runtime.internal;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;

/**
 * Utility for adapting Lettuce {@link CompletionStage} results to Mutiny {@link Uni}
 * and to blocking calls.
 * <p>
 * Lettuce async commands return {@link io.lettuce.core.RedisFuture} which extends
 * {@link CompletionStage}. This class provides the bridge to Quarkus APIs without
 * exposing any Reactor types.
 */
public final class LettuceResult {

    private LettuceResult() {
        // Utility class
    }

    /**
     * Converts a {@link CompletionStage} to a Mutiny {@link Uni}.
     * <p>
     * The returned {@code Uni} subscribes lazily — the {@code CompletionStage} supplier
     * is invoked only when a subscriber requests it.
     *
     * @param <T> the result type
     * @param supplier a supplier of the {@link CompletionStage} (typically a Lettuce async command call)
     * @return a {@link Uni} that completes with the result of the {@link CompletionStage}
     */
    public static <T> Uni<T> toUni(java.util.function.Supplier<? extends CompletionStage<T>> supplier) {
        return Uni.createFrom().completionStage(supplier);
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
