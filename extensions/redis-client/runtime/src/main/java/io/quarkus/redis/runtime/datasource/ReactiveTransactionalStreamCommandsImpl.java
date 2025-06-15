package io.quarkus.redis.runtime.datasource;

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
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveTransactionalStreamCommandsImpl<K, F, V> extends AbstractTransactionalCommands
        implements ReactiveTransactionalStreamCommands<K, F, V> {

    private final ReactiveStreamCommandsImpl<K, F, V> reactive;

    public ReactiveTransactionalStreamCommandsImpl(ReactiveTransactionalRedisDataSource ds,
            ReactiveStreamCommandsImpl<K, F, V> reactive, TransactionHolder tx) {
        super(ds, tx);
        this.reactive = reactive;
    }

    @Override
    public Uni<Void> xack(K key, String group, String... ids) {
        this.tx.enqueue(Response::toInteger);
        return this.reactive._xack(key, group, ids).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> xadd(K key, Map<F, V> payload) {
        this.tx.enqueue(Response::toString);
        return this.reactive._xadd(key, payload).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> xadd(K key, XAddArgs args, Map<F, V> payload) {
        this.tx.enqueue(Response::toString);
        return this.reactive._xadd(key, args, payload).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> xautoclaim(K key, String group, String consumer, Duration minIdleTime, String start) {
        this.tx.enqueue(r -> reactive.decodeAsClaimedMessages(key, r));
        return this.reactive._xautoclaim(key, group, consumer, minIdleTime, start).invoke(this::queuedOrDiscard)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> xautoclaim(K key, String group, String consumer, Duration minIdleTime, String start, int count) {
        this.tx.enqueue(r -> reactive.decodeAsClaimedMessages(key, r));
        return this.reactive._xautoclaim(key, group, consumer, minIdleTime, start, count).invoke(this::queuedOrDiscard)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> xautoclaim(K key, String group, String consumer, Duration minIdleTime, String start, int count,
            boolean justId) {
        this.tx.enqueue(r -> reactive.decodeAsClaimedMessages(key, r));
        return this.reactive._xautoclaim(key, group, consumer, minIdleTime, start, count, justId)
                .invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> xclaim(K key, String group, String consumer, Duration minIdleTime, String... id) {
        this.tx.enqueue(r -> reactive.decodeListOfMessages(key, r));
        return this.reactive._xclaim(key, group, consumer, minIdleTime, id).invoke(this::queuedOrDiscard)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> xclaim(K key, String group, String consumer, Duration minIdleTime, XClaimArgs args, String... id) {
        this.tx.enqueue(r -> reactive.decodeListOfMessages(key, r));
        return this.reactive._xclaim(key, group, consumer, minIdleTime, args, id).invoke(this::queuedOrDiscard)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> xdel(K key, String... id) {
        this.tx.enqueue(Response::toInteger);
        return this.reactive._xdel(key, id).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> xgroupCreate(K key, String groupname, String from) {
        this.tx.enqueue(r -> null);
        return this.reactive._xgroupCreate(key, groupname, from).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> xgroupCreate(K key, String groupname, String from, XGroupCreateArgs args) {
        this.tx.enqueue(r -> null);
        return this.reactive._xgroupCreate(key, groupname, from, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> xgroupCreateConsumer(K key, String groupname, String consumername) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._xgroupCreateConsumer(key, groupname, consumername).invoke(this::queuedOrDiscard)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> xgroupDelConsumer(K key, String groupname, String consumername) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._xgroupDelConsumer(key, groupname, consumername).invoke(this::queuedOrDiscard)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> xgroupDestroy(K key, String groupname) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._xgroupDestroy(key, groupname).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> xgroupSetId(K key, String groupname, String from) {
        this.tx.enqueue(r -> null);
        return this.reactive._xgroupSetId(key, groupname, from).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> xgroupSetId(K key, String groupname, String from, XGroupSetIdArgs args) {
        this.tx.enqueue(r -> null);
        return this.reactive._xgroupSetId(key, groupname, from, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> xlen(K key) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._xlen(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> xrange(K key, StreamRange range, int count) {
        this.tx.enqueue(r -> reactive.decodeListOfMessages(key, r));
        return this.reactive._xrange(key, range, count).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> xrange(K key, StreamRange range) {
        this.tx.enqueue(r -> reactive.decodeListOfMessages(key, r));
        return this.reactive._xrange(key, range).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> xread(K key, String id) {
        this.tx.enqueue(r -> reactive.decodeAsListOfMessagesFromXRead(r));
        return this.reactive._xread(key, id).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> xread(Map<K, String> lastIdsPerStream) {
        this.tx.enqueue(r -> reactive.decodeAsListOfMessagesFromXRead(r));
        return this.reactive._xread(lastIdsPerStream).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> xread(K key, String id, XReadArgs args) {
        this.tx.enqueue(r -> reactive.decodeAsListOfMessagesFromXRead(r));
        return this.reactive._xread(key, id, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> xread(Map<K, String> lastIdsPerStream, XReadArgs args) {
        this.tx.enqueue(r -> reactive.decodeAsListOfMessagesFromXRead(r));
        return this.reactive._xread(lastIdsPerStream, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> xreadgroup(String group, String consumer, K key, String id) {
        this.tx.enqueue(r -> reactive.decodeAsListOfMessagesFromXRead(r));
        return this.reactive._xreadgroup(group, consumer, key, id).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> xreadgroup(String group, String consumer, Map<K, String> lastIdsPerStream) {
        this.tx.enqueue(r -> reactive.decodeAsListOfMessagesFromXRead(r));
        return this.reactive._xreadgroup(group, consumer, lastIdsPerStream).invoke(this::queuedOrDiscard)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> xreadgroup(String group, String consumer, K key, String id, XReadGroupArgs args) {
        this.tx.enqueue(r -> reactive.decodeAsListOfMessagesFromXRead(r));
        return this.reactive._xreadgroup(group, consumer, key, id, args).invoke(this::queuedOrDiscard)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> xreadgroup(String group, String consumer, Map<K, String> lastIdsPerStream, XReadGroupArgs args) {
        this.tx.enqueue(r -> reactive.decodeAsListOfMessagesFromXRead(r));
        return this.reactive._xreadgroup(group, consumer, lastIdsPerStream, args).invoke(this::queuedOrDiscard)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> xrevrange(K key, StreamRange range, int count) {
        this.tx.enqueue(r -> reactive.decodeListOfMessages(key, r));
        return this.reactive._xrevrange(key, range, count).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> xrevrange(K key, StreamRange range) {
        this.tx.enqueue(r -> reactive.decodeListOfMessages(key, r));
        return this.reactive._xrevrange(key, range).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> xtrim(K key, String threshold) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._xtrim(key, new XTrimArgs().minid(threshold)).invoke(this::queuedOrDiscard)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> xtrim(K key, XTrimArgs args) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._xtrim(key, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> xpending(K key, String group) {
        this.tx.enqueue(reactive::decodeAsXPendingSummary);
        return this.reactive._xpending(key, group).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> xpending(K key, String group, StreamRange range, int count) {
        return this.xpending(key, group, range, count, null);
    }

    @Override
    public Uni<Void> xpending(K key, String group, StreamRange range, int count, XPendingArgs args) {
        this.tx.enqueue(reactive::decodeListOfPendingMessages);
        return this.reactive._xpending(key, group, range, count, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }
}
