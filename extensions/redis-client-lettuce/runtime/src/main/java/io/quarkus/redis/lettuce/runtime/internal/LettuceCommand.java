package io.quarkus.redis.lettuce.runtime.internal;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.lettuce.core.RedisFuture;
import io.smallrye.mutiny.Uni;

/**
 * A Lettuce command ready to be issued, paired with the mapper that converts its raw result
 * into the Quarkus-typed value and an optional hook run when the command fails. Shared by the
 * reactive and transactional paths so the mapping and the failure handling are defined exactly
 * once per command.
 * <p>
 * The failure hook is a side effect only: it observes the failure and cannot replace or recover it.
 * {@link #toUni()} runs it on a failed {@link Uni}, and the transactional path runs it on the error
 * the {@code EXEC} reply carries for a rejected command.
 *
 * @param <T> the raw Lettuce result type
 * @param <R> the Quarkus result type exposed on the public API
 */
public record LettuceCommand<T, R>(Supplier<RedisFuture<T>> call, Function<? super T, ? extends R> mapper,
        Consumer<Throwable> onFailure) {

    private static final Consumer<Throwable> NO_FAILURE_HOOK = ignored -> {
    };

    public LettuceCommand {
        nonNull(call, "call");
        nonNull(mapper, "mapper");
        nonNull(onFailure, "onFailure");
    }

    public LettuceCommand(Supplier<RedisFuture<T>> call, Function<? super T, ? extends R> mapper) {
        this(call, mapper, NO_FAILURE_HOOK);
    }

    public Uni<R> toUni() {
        Uni<R> uni = LettuceResult.toUni(call).map(mapper);
        if (onFailure == NO_FAILURE_HOOK) {
            return uni;
        }
        return uni.onFailure().invoke(onFailure);
    }

    public LettuceCommand<T, R> onFailure(Consumer<Throwable> hook) {
        return new LettuceCommand<>(call, mapper, hook);
    }

    public LettuceCommand<T, Void> discarding() {
        return new LettuceCommand<>(call, ignored -> null, onFailure);
    }

    public static <T, R> LettuceCommand<T, R> failing(RuntimeException failure) {
        nonNull(failure, "failure");
        return new LettuceCommand<>(() -> {
            throw failure;
        }, ignored -> null);
    }

    public static <T, R> LettuceCommand<T, R> of(Supplier<RedisFuture<T>> call, Function<? super T, ? extends R> mapper) {
        return new LettuceCommand<>(call, mapper);
    }

    public static <T> LettuceCommand<T, T> of(Supplier<RedisFuture<T>> call) {
        return new LettuceCommand<>(call, Function.identity());
    }

    public static <T> LettuceCommand<T, Void> discarding(Supplier<RedisFuture<T>> call) {
        return new LettuceCommand<>(call, ignored -> null);
    }

}
