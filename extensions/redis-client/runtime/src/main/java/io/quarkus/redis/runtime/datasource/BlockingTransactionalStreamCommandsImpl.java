package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.util.Map;

import io.quarkus.redis.datasource.stream.ReactiveTransactionalStreamCommands;
import io.quarkus.redis.datasource.stream.StreamRange;
import io.quarkus.redis.datasource.stream.TransactionalStreamCommands;
import io.quarkus.redis.datasource.stream.XAddArgs;
import io.quarkus.redis.datasource.stream.XClaimArgs;
import io.quarkus.redis.datasource.stream.XGroupCreateArgs;
import io.quarkus.redis.datasource.stream.XGroupSetIdArgs;
import io.quarkus.redis.datasource.stream.XPendingArgs;
import io.quarkus.redis.datasource.stream.XReadArgs;
import io.quarkus.redis.datasource.stream.XReadGroupArgs;
import io.quarkus.redis.datasource.stream.XTrimArgs;
import io.quarkus.redis.datasource.transactions.TransactionalRedisDataSource;

public class BlockingTransactionalStreamCommandsImpl<K, F, V> extends AbstractTransactionalRedisCommandGroup
        implements TransactionalStreamCommands<K, F, V> {

    private final ReactiveTransactionalStreamCommands<K, F, V> reactive;

    public BlockingTransactionalStreamCommandsImpl(TransactionalRedisDataSource ds,
            ReactiveTransactionalStreamCommands<K, F, V> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public void xack(K key, String group, String... ids) {
        reactive.xack(key, group, ids).await().atMost(timeout);
    }

    @Override
    public void xadd(K key, Map<F, V> payload) {
        reactive.xadd(key, payload).await().atMost(timeout);
    }

    @Override
    public void xadd(K key, XAddArgs args, Map<F, V> payload) {
        reactive.xadd(key, args, payload).await().atMost(timeout);
    }

    @Override
    public void xautoclaim(K key, String group, String consumer, Duration minIdleTime, String start) {
        reactive.xautoclaim(key, group, consumer, minIdleTime, start).await().atMost(timeout);
    }

    @Override
    public void xautoclaim(K key, String group, String consumer, Duration minIdleTime, String start, int count) {
        reactive.xautoclaim(key, group, consumer, minIdleTime, start, count).await().atMost(timeout);
    }

    @Override
    public void xautoclaim(K key, String group, String consumer, Duration minIdleTime, String start, int count,
            boolean justId) {
        reactive.xautoclaim(key, group, consumer, minIdleTime, start, count, justId).await().atMost(timeout);
    }

    @Override
    public void xclaim(K key, String group, String consumer, Duration minIdleTime, String... id) {
        reactive.xclaim(key, group, consumer, minIdleTime, id).await().atMost(timeout);
    }

    @Override
    public void xclaim(K key, String group, String consumer, Duration minIdleTime, XClaimArgs args, String... id) {
        reactive.xclaim(key, group, consumer, minIdleTime, args, id).await().atMost(timeout);
    }

    @Override
    public void xdel(K key, String... id) {
        reactive.xdel(key, id).await().atMost(timeout);
    }

    @Override
    public void xgroupCreate(K key, String groupname, String from) {
        reactive.xgroupCreate(key, groupname, from).await().atMost(timeout);
    }

    @Override
    public void xgroupCreate(K key, String groupname, String from, XGroupCreateArgs args) {
        reactive.xgroupCreate(key, groupname, from, args).await().atMost(timeout);
    }

    @Override
    public void xgroupCreateConsumer(K key, String groupname, String consumername) {
        reactive.xgroupCreateConsumer(key, groupname, consumername).await().atMost(timeout);
    }

    @Override
    public void xgroupDelConsumer(K key, String groupname, String consumername) {
        reactive.xgroupDelConsumer(key, groupname, consumername).await().atMost(timeout);
    }

    @Override
    public void xgroupDestroy(K key, String groupname) {
        reactive.xgroupDestroy(key, groupname).await().atMost(timeout);
    }

    @Override
    public void xgroupSetId(K key, String groupname, String from) {
        reactive.xgroupSetId(key, groupname, from).await().atMost(timeout);
    }

    @Override
    public void xgroupSetId(K key, String groupname, String from, XGroupSetIdArgs args) {
        reactive.xgroupSetId(key, groupname, from, args).await().atMost(timeout);
    }

    @Override
    public void xlen(K key) {
        reactive.xlen(key).await().atMost(timeout);
    }

    @Override
    public void xrange(K key, StreamRange range, int count) {
        reactive.xrange(key, range, count).await().atMost(timeout);
    }

    @Override
    public void xrange(K key, StreamRange range) {
        reactive.xrange(key, range).await().atMost(timeout);
    }

    @Override
    public void xread(K key, String id) {
        reactive.xread(key, id).await().atMost(timeout);
    }

    @Override
    public void xread(Map<K, String> lastIdsPerStream) {
        reactive.xread(lastIdsPerStream).await().atMost(timeout);
    }

    @Override
    public void xread(K key, String id, XReadArgs args) {
        reactive.xread(key, id, args).await().atMost(timeout);
    }

    @Override
    public void xread(Map<K, String> lastIdsPerStream, XReadArgs args) {
        reactive.xread(lastIdsPerStream, args).await().atMost(timeout);
    }

    @Override
    public void xreadgroup(String group, String consumer, K key, String id) {
        reactive.xreadgroup(group, consumer, key, id).await().atMost(timeout);
    }

    @Override
    public void xreadgroup(String group, String consumer, Map<K, String> lastIdsPerStream) {
        reactive.xreadgroup(group, consumer, lastIdsPerStream).await().atMost(timeout);
    }

    @Override
    public void xreadgroup(String group, String consumer, K key, String id, XReadGroupArgs args) {
        reactive.xreadgroup(group, consumer, key, id, args).await().atMost(timeout);
    }

    @Override
    public void xreadgroup(String group, String consumer, Map<K, String> lastIdsPerStream, XReadGroupArgs args) {
        reactive.xreadgroup(group, consumer, lastIdsPerStream, args).await().atMost(timeout);
    }

    @Override
    public void xrevrange(K key, StreamRange range, int count) {
        reactive.xrevrange(key, range, count).await().atMost(timeout);
    }

    @Override
    public void xrevrange(K key, StreamRange range) {
        reactive.xrevrange(key, range).await().atMost(timeout);
    }

    @Override
    public void xtrim(K key, String threshold) {
        reactive.xtrim(key, threshold).await().atMost(timeout);
    }

    @Override
    public void xtrim(K key, XTrimArgs args) {
        reactive.xtrim(key, args).await().atMost(timeout);
    }

    @Override
    public void xpending(K key, String group) {
        reactive.xpending(key, group).await().atMost(timeout);
    }

    @Override
    public void xpending(K key, String group, StreamRange range, int count) {
        reactive.xpending(key, group, range, count).await().atMost(timeout);
    }

    @Override
    public void xpending(K key, String group, StreamRange range, int count, XPendingArgs args) {
        reactive.xpending(key, group, range, count, args).await().atMost(timeout);
    }
}
