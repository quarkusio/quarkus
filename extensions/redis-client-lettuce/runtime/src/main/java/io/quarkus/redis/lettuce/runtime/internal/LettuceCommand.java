package io.quarkus.redis.lettuce.runtime.internal;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Function;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.smallrye.mutiny.Uni;

/**
 * A Lettuce command ready to be issued: a call on the asynchronous commands of a connection, paired with the mapper
 * that converts its raw result into the Quarkus-typed value, and bound to the {@link LettuceConnectionSource} of the
 * command group that built it. Shared by the reactive and transactional paths so the mapping is defined exactly once
 * per command: {@link #toUni()} issues it on the connection of its source, a transaction issues it on its own
 * connection with {@link #issueOn(LettuceConnection)}.
 *
 * @param <T> the raw Lettuce result type
 * @param <R> the Quarkus result type exposed on the public API
 */
public final class LettuceCommand<T, R> {

    private final LettuceConnectionSource source;
    private final Function<RedisClusterAsyncCommands<byte[], byte[]>, RedisFuture<T>> call;
    private final Function<? super T, ? extends R> mapper;
    /** What a command that cannot be issued fails with, see {@link #failing(RuntimeException)}. */
    private final RuntimeException failure;

    private LettuceCommand(LettuceConnectionSource source,
            Function<RedisClusterAsyncCommands<byte[], byte[]>, RedisFuture<T>> call,
            Function<? super T, ? extends R> mapper, RuntimeException failure) {
        this.source = source;
        this.call = call;
        this.mapper = mapper;
        this.failure = failure;
    }

    public static <T, R> LettuceCommand<T, R> of(LettuceConnectionSource source,
            Function<RedisClusterAsyncCommands<byte[], byte[]>, RedisFuture<T>> call,
            Function<? super T, ? extends R> mapper) {
        return new LettuceCommand<>(nonNull(source, "source"), nonNull(call, "call"), nonNull(mapper, "mapper"), null);
    }

    public static <T> LettuceCommand<T, T> of(LettuceConnectionSource source,
            Function<RedisClusterAsyncCommands<byte[], byte[]>, RedisFuture<T>> call) {
        return of(source, call, Function.identity());
    }

    public static <T> LettuceCommand<T, Void> discarding(LettuceConnectionSource source,
            Function<RedisClusterAsyncCommands<byte[], byte[]>, RedisFuture<T>> call) {
        return of(source, call, ignored -> null);
    }

    /**
     * A command that fails with {@code failure} instead of being issued: the command implementations defer some
     * argument validations that way. It needs no connection, and does not wait for one.
     */
    public static <T, R> LettuceCommand<T, R> failing(RuntimeException failure) {
        return new LettuceCommand<>(null, null, ignored -> null, nonNull(failure, "failure"));
    }

    /**
     * Issues the command on the connection of its source when subscribed, see
     * {@link LettuceResult#toUni(LettuceConnectionSource, Function)}.
     */
    public Uni<R> toUni() {
        if (failure != null) {
            return Uni.createFrom().failure(failure);
        }
        return LettuceResult.toUni(source, connection -> call.apply(connection.async())).map(mapper);
    }

    /**
     * Issues the command on {@code connection} at once, whatever its source: what a transaction does on the
     * connection it runs on.
     *
     * @throws RuntimeException the failure of a {@link #failing(RuntimeException) failing} command
     */
    public RedisFuture<T> issueOn(LettuceConnection connection) {
        if (failure != null) {
            throw failure;
        }
        return call.apply(connection.async());
    }

    public Function<? super T, ? extends R> mapper() {
        return mapper;
    }

    /**
     * The same command, bound to another source: a connection borrowed from the pool for a blocking command.
     */
    public LettuceCommand<T, R> on(LettuceConnectionSource other) {
        return failure != null ? this : new LettuceCommand<>(nonNull(other, "source"), call, mapper, null);
    }

    /**
     * The same command, its raw result kept.
     */
    public LettuceCommand<T, T> raw() {
        return new LettuceCommand<>(source, call, Function.identity(), failure);
    }

    /**
     * The same command, its result discarded.
     */
    public LettuceCommand<T, Void> discarding() {
        return new LettuceCommand<>(source, call, ignored -> null, failure);
    }

}
