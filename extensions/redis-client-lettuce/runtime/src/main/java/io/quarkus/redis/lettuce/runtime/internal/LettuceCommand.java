package io.quarkus.redis.lettuce.runtime.internal;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Function;
import java.util.function.Supplier;

import io.lettuce.core.RedisFuture;
import io.smallrye.mutiny.Uni;

/**
 * A Lettuce command ready to be issued, paired with the mapper that converts its raw result
 * into the Quarkus-typed value. Shared by the reactive and transactional paths so the mapping
 * is defined exactly once per command.
 *
 * @param <T> the raw Lettuce result type
 * @param <R> the Quarkus result type exposed on the public API
 */
public record LettuceCommand<T, R>(Supplier<RedisFuture<T>> call, Function<? super T, ? extends R> mapper) {

    public LettuceCommand {
        nonNull(call, "call");
        nonNull(mapper, "mapper");
    }

    public Uni<R> toUni() {
        return LettuceResult.toUni(call).map(mapper);
    }

    public LettuceCommand<T, Void> discarding() {
        return discarding(call);
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
