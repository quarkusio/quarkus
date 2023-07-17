package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.stream.ClaimedMessages;
import io.quarkus.redis.datasource.stream.PendingMessage;
import io.quarkus.redis.datasource.stream.ReactiveStreamCommands;
import io.quarkus.redis.datasource.stream.StreamCommands;
import io.quarkus.redis.datasource.stream.StreamMessage;
import io.quarkus.redis.datasource.stream.StreamRange;
import io.quarkus.redis.datasource.stream.XAddArgs;
import io.quarkus.redis.datasource.stream.XClaimArgs;
import io.quarkus.redis.datasource.stream.XGroupCreateArgs;
import io.quarkus.redis.datasource.stream.XGroupSetIdArgs;
import io.quarkus.redis.datasource.stream.XPendingArgs;
import io.quarkus.redis.datasource.stream.XPendingSummary;
import io.quarkus.redis.datasource.stream.XReadArgs;
import io.quarkus.redis.datasource.stream.XReadGroupArgs;
import io.quarkus.redis.datasource.stream.XTrimArgs;

public class BlockingStreamCommandsImpl<K, F, V> extends AbstractRedisCommandGroup implements StreamCommands<K, F, V> {

    private final ReactiveStreamCommands<K, F, V> reactive;

    public BlockingStreamCommandsImpl(RedisDataSource ds, ReactiveStreamCommands<K, F, V> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public int xack(K key, String group, String... ids) {
        return reactive.xack(key, group, ids).await().atMost(timeout);
    }

    @Override
    public String xadd(K key, Map<F, V> payload) {
        return reactive.xadd(key, payload).await().atMost(timeout);
    }

    @Override
    public String xadd(K key, XAddArgs args, Map<F, V> payload) {
        return reactive.xadd(key, args, payload).await().atMost(timeout);
    }

    @Override
    public ClaimedMessages<K, F, V> xautoclaim(K key, String group, String consumer, Duration minIdleTime, String start,
            int count) {
        return reactive.xautoclaim(key, group, consumer, minIdleTime, start, count).await().atMost(timeout);
    }

    @Override
    public ClaimedMessages<K, F, V> xautoclaim(K key, String group, String consumer, Duration minIdleTime, String start) {
        return reactive.xautoclaim(key, group, consumer, minIdleTime, start).await().atMost(timeout);
    }

    @Override
    public ClaimedMessages<K, F, V> xautoclaim(K key, String group, String consumer, Duration minIdleTime, String start,
            int count, boolean justId) {
        return reactive.xautoclaim(key, group, consumer, minIdleTime, start, count, justId).await().atMost(timeout);
    }

    @Override
    public List<StreamMessage<K, F, V>> xclaim(K key, String group, String consumer, Duration minIdleTime, String... id) {
        return reactive.xclaim(key, group, consumer, minIdleTime, id).await().atMost(timeout);
    }

    @Override
    public List<StreamMessage<K, F, V>> xclaim(K key, String group, String consumer, Duration minIdleTime, XClaimArgs args,
            String... id) {
        return reactive.xclaim(key, group, consumer, minIdleTime, args, id).await().atMost(timeout);
    }

    @Override
    public int xdel(K key, String... id) {
        return reactive.xdel(key, id).await().atMost(timeout);
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
    public boolean xgroupCreateConsumer(K key, String groupname, String consumername) {
        return reactive.xgroupCreateConsumer(key, groupname, consumername).await().atMost(timeout);
    }

    @Override
    public long xgroupDelConsumer(K key, String groupname, String consumername) {
        return reactive.xgroupDelConsumer(key, groupname, consumername).await().atMost(timeout);
    }

    @Override
    public boolean xgroupDestroy(K key, String groupname) {
        return reactive.xgroupDestroy(key, groupname).await().atMost(timeout);
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
    public long xlen(K key) {
        return reactive.xlen(key).await().atMost(timeout);
    }

    @Override
    public List<StreamMessage<K, F, V>> xrange(K key, StreamRange range, int count) {
        return reactive.xrange(key, range, count).await().atMost(timeout);
    }

    @Override
    public List<StreamMessage<K, F, V>> xrange(K key, StreamRange range) {
        return reactive.xrange(key, range).await().atMost(timeout);
    }

    @Override
    public List<StreamMessage<K, F, V>> xread(K key, String id) {
        return reactive.xread(key, id).await().atMost(timeout);
    }

    @Override
    public List<StreamMessage<K, F, V>> xread(Map<K, String> lastIdsPerStream) {
        return reactive.xread(lastIdsPerStream).await().atMost(timeout);
    }

    @Override
    public List<StreamMessage<K, F, V>> xread(K key, String id, XReadArgs args) {
        return reactive.xread(key, id, args).await().atMost(timeout);
    }

    @Override
    public List<StreamMessage<K, F, V>> xread(Map<K, String> lastIdsPerStream, XReadArgs args) {
        return reactive.xread(lastIdsPerStream, args).await().atMost(timeout);
    }

    @Override
    public List<StreamMessage<K, F, V>> xreadgroup(String group, String consumer, K key, String id) {
        return reactive.xreadgroup(group, consumer, key, id).await().atMost(timeout);
    }

    @Override
    public List<StreamMessage<K, F, V>> xreadgroup(String group, String consumer, Map<K, String> lastIdsPerStream) {
        return reactive.xreadgroup(group, consumer, lastIdsPerStream).await().atMost(timeout);
    }

    @Override
    public List<StreamMessage<K, F, V>> xreadgroup(String group, String consumer, K key, String id, XReadGroupArgs args) {
        return reactive.xreadgroup(group, consumer, key, id, args).await().atMost(timeout);
    }

    @Override
    public List<StreamMessage<K, F, V>> xreadgroup(String group, String consumer, Map<K, String> lastIdsPerStream,
            XReadGroupArgs args) {
        return reactive.xreadgroup(group, consumer, lastIdsPerStream, args).await().atMost(timeout);
    }

    @Override
    public List<StreamMessage<K, F, V>> xrevrange(K key, StreamRange range, int count) {
        return reactive.xrevrange(key, range, count).await().atMost(timeout);
    }

    @Override
    public List<StreamMessage<K, F, V>> xrevrange(K key, StreamRange range) {
        return reactive.xrevrange(key, range).await().atMost(timeout);
    }

    @Override
    public long xtrim(K key, String threshold) {
        return reactive.xtrim(key, threshold).await().atMost(timeout);
    }

    @Override
    public long xtrim(K key, XTrimArgs args) {
        return reactive.xtrim(key, args).await().atMost(timeout);
    }

    @Override
    public XPendingSummary xpending(K key, String group) {
        return reactive.xpending(key, group).await().atMost(timeout);
    }

    @Override
    public List<PendingMessage> xpending(K key, String group, StreamRange range, int count) {
        return reactive.xpending(key, group, range, count).await().atMost(timeout);
    }

    @Override
    public List<PendingMessage> xpending(K key, String group, StreamRange range, int count, XPendingArgs args) {
        return reactive.xpending(key, group, range, count, args).await().atMost(timeout);
    }
}
