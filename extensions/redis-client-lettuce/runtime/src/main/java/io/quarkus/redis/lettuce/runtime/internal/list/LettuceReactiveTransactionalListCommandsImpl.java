package io.quarkus.redis.lettuce.runtime.internal.list;

import java.time.Duration;

import io.quarkus.redis.datasource.list.LPosArgs;
import io.quarkus.redis.datasource.list.Position;
import io.quarkus.redis.datasource.list.ReactiveTransactionalListCommands;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.quarkus.redis.lettuce.runtime.internal.datasource.LettuceTransactionHolder;
import io.smallrye.mutiny.Uni;

/**
 * Lettuce-backed implementation of {@link ReactiveTransactionalListCommands}.
 * <p>
 * A thin shell over {@link LettuceReactiveListCommandsImpl}. Each command reuses the non-transactional
 * {@code _xxx(...)} seam for validation and conversion, and hands the resulting
 * {@link io.quarkus.redis.lettuce.runtime.internal.LettuceCommand} to the {@link LettuceTransactionHolder}.
 * The command carries the same result mapper the non-transactional path applies, so
 * {@code TransactionResult.get(index)} yields the same type the non-transactional call returns.
 * {@code SORT} is not part of the transactional API.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class LettuceReactiveTransactionalListCommandsImpl<K, V> implements ReactiveTransactionalListCommands<K, V> {

    private final ReactiveTransactionalRedisDataSource dataSource;
    private final LettuceReactiveListCommandsImpl<K, V> reactive;
    private final LettuceTransactionHolder tx;

    public LettuceReactiveTransactionalListCommandsImpl(ReactiveTransactionalRedisDataSource dataSource,
            LettuceReactiveListCommandsImpl<K, V> reactive,
            LettuceTransactionHolder tx) {
        this.dataSource = dataSource;
        this.reactive = reactive;
        this.tx = tx;
    }

    @Override
    public ReactiveTransactionalRedisDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Uni<Void> blmove(K source, K destination, Position positionInSource, Position positionInDest,
            Duration timeout) {
        return tx.enqueue(reactive._blmove(source, destination, positionInSource, positionInDest, timeout));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> blmpop(Duration timeout, Position position, K... keys) {
        return tx.enqueue(reactive._blmpop(timeout, position, keys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> blmpop(Duration timeout, Position position, int count, K... keys) {
        return tx.enqueue(reactive._blmpop(timeout, position, count, keys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> blpop(Duration timeout, K... keys) {
        return tx.enqueue(reactive._blpop(timeout, keys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> brpop(Duration timeout, K... keys) {
        return tx.enqueue(reactive._brpop(timeout, keys));
    }

    @Deprecated
    @Override
    public Uni<Void> brpoplpush(Duration timeout, K source, K destination) {
        return tx.enqueue(reactive._brpoplpush(timeout, source, destination));
    }

    @Override
    public Uni<Void> lindex(K key, long index) {
        return tx.enqueue(reactive._lindex(key, index));
    }

    @Override
    public Uni<Void> linsertBeforePivot(K key, V pivot, V element) {
        return tx.enqueue(reactive._linsertBeforePivot(key, pivot, element));
    }

    @Override
    public Uni<Void> linsertAfterPivot(K key, V pivot, V element) {
        return tx.enqueue(reactive._linsertAfterPivot(key, pivot, element));
    }

    @Override
    public Uni<Void> llen(K key) {
        return tx.enqueue(reactive._llen(key));
    }

    @Override
    public Uni<Void> lmove(K source, K destination, Position positionInSource, Position positionInDestination) {
        return tx.enqueue(reactive._lmove(source, destination, positionInSource, positionInDestination));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> lmpop(Position position, K... keys) {
        return tx.enqueue(reactive._lmpop(position, keys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> lmpop(Position position, int count, K... keys) {
        return tx.enqueue(reactive._lmpop(position, count, keys));
    }

    @Override
    public Uni<Void> lpop(K key) {
        return tx.enqueue(reactive._lpop(key));
    }

    @Override
    public Uni<Void> lpop(K key, int count) {
        return tx.enqueue(reactive._lpop(key, count));
    }

    @Override
    public Uni<Void> lpos(K key, V element) {
        return tx.enqueue(reactive._lpos(key, element));
    }

    @Override
    public Uni<Void> lpos(K key, V element, LPosArgs args) {
        return tx.enqueue(reactive._lpos(key, element, args));
    }

    @Override
    public Uni<Void> lpos(K key, V element, int count) {
        return tx.enqueue(reactive._lpos(key, element, count));
    }

    @Override
    public Uni<Void> lpos(K key, V element, int count, LPosArgs args) {
        return tx.enqueue(reactive._lpos(key, element, count, args));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> lpush(K key, V... elements) {
        return tx.enqueue(reactive._lpush(key, elements));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> lpushx(K key, V... elements) {
        return tx.enqueue(reactive._lpushx(key, elements));
    }

    @Override
    public Uni<Void> lrange(K key, long start, long stop) {
        return tx.enqueue(reactive._lrange(key, start, stop));
    }

    @Override
    public Uni<Void> lrem(K key, long count, V element) {
        return tx.enqueue(reactive._lrem(key, count, element));
    }

    @Override
    public Uni<Void> lset(K key, long index, V element) {
        return tx.enqueue(reactive._lset(key, index, element));
    }

    @Override
    public Uni<Void> ltrim(K key, long start, long stop) {
        return tx.enqueue(reactive._ltrim(key, start, stop));
    }

    @Override
    public Uni<Void> rpop(K key) {
        return tx.enqueue(reactive._rpop(key));
    }

    @Override
    public Uni<Void> rpop(K key, int count) {
        return tx.enqueue(reactive._rpop(key, count));
    }

    @Deprecated
    @Override
    public Uni<Void> rpoplpush(K source, K destination) {
        return tx.enqueue(reactive._rpoplpush(source, destination));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> rpush(K key, V... values) {
        return tx.enqueue(reactive._rpush(key, values));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> rpushx(K key, V... values) {
        return tx.enqueue(reactive._rpushx(key, values));
    }
}
