package io.quarkus.redis.lettuce.runtime.internal.stream;

import java.time.Duration;
import java.util.Map;

import io.quarkus.redis.datasource.stream.ReactiveTransactionalStreamCommands;
import io.quarkus.redis.datasource.stream.StreamRange;
import io.quarkus.redis.datasource.stream.XAddArgs;
import io.quarkus.redis.datasource.stream.XClaimArgs;
import io.quarkus.redis.datasource.stream.XGroupCreateArgs;
import io.quarkus.redis.datasource.stream.XGroupSetIdArgs;
import io.quarkus.redis.datasource.stream.XPendingArgs;
import io.quarkus.redis.datasource.stream.XReadArgs;
import io.quarkus.redis.datasource.stream.XReadGroupArgs;
import io.quarkus.redis.datasource.stream.XTrimArgs;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.quarkus.redis.lettuce.runtime.internal.datasource.LettuceTransactionHolder;
import io.smallrye.mutiny.Uni;

/**
 * Lettuce-backed implementation of {@link ReactiveTransactionalStreamCommands}. Every command is queued on the
 * transaction's pinned connection, including a blocking {@code XREAD}/{@code XREADGROUP}: inside {@code MULTI}
 * Redis answers them at once without blocking.
 *
 * @param <K> the key type
 * @param <F> the field type
 * @param <V> the value type
 */
public class LettuceReactiveTransactionalStreamCommandsImpl<K, F, V> implements ReactiveTransactionalStreamCommands<K, F, V> {

    private final ReactiveTransactionalRedisDataSource dataSource;
    private final LettuceReactiveStreamCommandsImpl<K, F, V> reactive;
    private final LettuceTransactionHolder tx;

    public LettuceReactiveTransactionalStreamCommandsImpl(ReactiveTransactionalRedisDataSource dataSource,
            LettuceReactiveStreamCommandsImpl<K, F, V> reactive, LettuceTransactionHolder tx) {
        this.dataSource = dataSource;
        this.reactive = reactive;
        this.tx = tx;
    }

    @Override
    public ReactiveTransactionalRedisDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Uni<Void> xack(K key, String group, String... ids) {
        return tx.enqueue(reactive._xack(key, group, ids));
    }

    @Override
    public Uni<Void> xadd(K key, Map<F, V> payload) {
        return tx.enqueue(reactive._xadd(key, payload));
    }

    @Override
    public Uni<Void> xadd(K key, XAddArgs args, Map<F, V> payload) {
        return tx.enqueue(reactive._xadd(key, args, payload));
    }

    @Override
    public Uni<Void> xautoclaim(K key, String group, String consumer, Duration minIdleTime, String start) {
        return tx.enqueue(reactive._xautoclaim(key, group, consumer, minIdleTime, start));
    }

    @Override
    public Uni<Void> xautoclaim(K key, String group, String consumer, Duration minIdleTime, String start, int count) {
        return tx.enqueue(reactive._xautoclaim(key, group, consumer, minIdleTime, start, count));
    }

    @Override
    public Uni<Void> xautoclaim(K key, String group, String consumer, Duration minIdleTime, String start, int count,
            boolean justId) {
        return tx.enqueue(reactive._xautoclaim(key, group, consumer, minIdleTime, start, count, justId));
    }

    @Override
    public Uni<Void> xclaim(K key, String group, String consumer, Duration minIdleTime, String... id) {
        return tx.enqueue(reactive._xclaim(key, group, consumer, minIdleTime, id));
    }

    @Override
    public Uni<Void> xclaim(K key, String group, String consumer, Duration minIdleTime, XClaimArgs args, String... id) {
        return tx.enqueue(reactive._xclaim(key, group, consumer, minIdleTime, args, id));
    }

    @Override
    public Uni<Void> xdel(K key, String... id) {
        return tx.enqueue(reactive._xdel(key, id));
    }

    @Override
    public Uni<Void> xgroupCreate(K key, String groupname, String from) {
        return tx.enqueue(reactive._xgroupCreate(key, groupname, from));
    }

    @Override
    public Uni<Void> xgroupCreate(K key, String groupname, String from, XGroupCreateArgs args) {
        return tx.enqueue(reactive._xgroupCreate(key, groupname, from, args));
    }

    @Override
    public Uni<Void> xgroupCreateConsumer(K key, String groupname, String consumername) {
        return tx.enqueue(reactive._xgroupCreateConsumer(key, groupname, consumername));
    }

    @Override
    public Uni<Void> xgroupDelConsumer(K key, String groupname, String consumername) {
        return tx.enqueue(reactive._xgroupDelConsumer(key, groupname, consumername));
    }

    @Override
    public Uni<Void> xgroupDestroy(K key, String groupname) {
        return tx.enqueue(reactive._xgroupDestroy(key, groupname));
    }

    @Override
    public Uni<Void> xgroupSetId(K key, String groupname, String from) {
        return tx.enqueue(reactive._xgroupSetId(key, groupname, from));
    }

    @Override
    public Uni<Void> xgroupSetId(K key, String groupname, String from, XGroupSetIdArgs args) {
        return tx.enqueue(reactive._xgroupSetId(key, groupname, from, args));
    }

    @Override
    public Uni<Void> xlen(K key) {
        return tx.enqueue(reactive._xlen(key));
    }

    @Override
    public Uni<Void> xrange(K key, StreamRange range, int count) {
        return tx.enqueue(reactive._xrange(key, range, count));
    }

    @Override
    public Uni<Void> xrange(K key, StreamRange range) {
        return tx.enqueue(reactive._xrange(key, range));
    }

    @Override
    public Uni<Void> xread(K key, String id) {
        return tx.enqueue(reactive._xread(key, id));
    }

    @Override
    public Uni<Void> xread(Map<K, String> lastIdsPerStream) {
        return tx.enqueue(reactive._xread(lastIdsPerStream));
    }

    @Override
    public Uni<Void> xread(K key, String id, XReadArgs args) {
        return tx.enqueue(reactive._xread(key, id, args));
    }

    @Override
    public Uni<Void> xread(Map<K, String> lastIdsPerStream, XReadArgs args) {
        return tx.enqueue(reactive._xread(lastIdsPerStream, args));
    }

    @Override
    public Uni<Void> xreadgroup(String group, String consumer, K key, String id) {
        return tx.enqueue(reactive._xreadgroup(group, consumer, key, id));
    }

    @Override
    public Uni<Void> xreadgroup(String group, String consumer, Map<K, String> lastIdsPerStream) {
        return tx.enqueue(reactive._xreadgroup(group, consumer, lastIdsPerStream));
    }

    @Override
    public Uni<Void> xreadgroup(String group, String consumer, K key, String id, XReadGroupArgs args) {
        return tx.enqueue(reactive._xreadgroup(group, consumer, key, id, args));
    }

    @Override
    public Uni<Void> xreadgroup(String group, String consumer, Map<K, String> lastIdsPerStream, XReadGroupArgs args) {
        return tx.enqueue(reactive._xreadgroup(group, consumer, lastIdsPerStream, args));
    }

    @Override
    public Uni<Void> xrevrange(K key, StreamRange range, int count) {
        return tx.enqueue(reactive._xrevrange(key, range, count));
    }

    @Override
    public Uni<Void> xrevrange(K key, StreamRange range) {
        return tx.enqueue(reactive._xrevrange(key, range));
    }

    @Override
    public Uni<Void> xtrim(K key, String threshold) {
        return tx.enqueue(reactive._xtrim(key, threshold));
    }

    @Override
    public Uni<Void> xtrim(K key, XTrimArgs args) {
        return tx.enqueue(reactive._xtrim(key, args));
    }

    @Override
    public Uni<Void> xpending(K key, String group) {
        return tx.enqueue(reactive._xpending(key, group));
    }

    @Override
    public Uni<Void> xpending(K key, String group, StreamRange range, int count) {
        return tx.enqueue(reactive._xpending(key, group, range, count, null));
    }

    @Override
    public Uni<Void> xpending(K key, String group, StreamRange range, int count, XPendingArgs args) {
        return tx.enqueue(reactive._xpending(key, group, range, count, args));
    }

}
