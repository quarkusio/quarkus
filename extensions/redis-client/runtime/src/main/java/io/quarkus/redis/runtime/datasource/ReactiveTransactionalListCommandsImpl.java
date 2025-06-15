package io.quarkus.redis.runtime.datasource;

import java.time.Duration;

import io.quarkus.redis.datasource.list.LPosArgs;
import io.quarkus.redis.datasource.list.Position;
import io.quarkus.redis.datasource.list.ReactiveTransactionalListCommands;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveTransactionalListCommandsImpl<K, V> extends AbstractTransactionalCommands
        implements ReactiveTransactionalListCommands<K, V> {

    private final ReactiveListCommandsImpl<K, V> reactive;

    public ReactiveTransactionalListCommandsImpl(ReactiveTransactionalRedisDataSource ds,
            ReactiveListCommandsImpl<K, V> reactive, TransactionHolder tx) {
        super(ds, tx);
        this.reactive = reactive;
    }

    @Override
    public Uni<Void> blmove(K source, K destination, Position positionInSource, Position positionInDest,
            Duration timeout) {
        this.tx.enqueue(this.reactive::decodeV);
        return this.reactive._blmove(source, destination, positionInSource, positionInDest, timeout)
                .invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> blmpop(Duration timeout, Position position, K... keys) {
        this.tx.enqueue(this.reactive::decodeKeyValueWithList);
        return this.reactive._blmpop(timeout, position, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> blmpop(Duration timeout, Position position, int count, K... keys) {
        this.tx.enqueue(this.reactive::decodeListOfKeyValue);
        return this.reactive._blmpop(timeout, position, count, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> blpop(Duration timeout, K... keys) {
        this.tx.enqueue(this.reactive::decodeKeyValue);
        return this.reactive._blpop(timeout, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> brpop(Duration timeout, K... keys) {
        this.tx.enqueue(this.reactive::decodeKeyValue);
        return this.reactive._brpop(timeout, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Deprecated
    @Override
    public Uni<Void> brpoplpush(Duration timeout, K source, K destination) {
        this.tx.enqueue(this.reactive::decodeV);
        return this.reactive._brpoplpush(timeout, source, destination).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> lindex(K key, long index) {
        this.tx.enqueue(this.reactive::decodeV);
        return this.reactive._lindex(key, index).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> linsertBeforePivot(K key, V pivot, V element) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._linsertBeforePivot(key, pivot, element).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> linsertAfterPivot(K key, V pivot, V element) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._linsertAfterPivot(key, pivot, element).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> llen(K key) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._llen(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> lmove(K source, K destination, Position positionInSource, Position positionInDestination) {
        this.tx.enqueue(this.reactive::decodeV);
        return this.reactive._lmove(source, destination, positionInSource, positionInDestination)
                .invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> lmpop(Position position, K... keys) {
        this.tx.enqueue(this.reactive::decodeKeyValueWithList);
        return this.reactive._lmpop(position, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> lmpop(Position position, int count, K... keys) {
        this.tx.enqueue(this.reactive::decodeListOfKeyValue);
        return this.reactive._lmpop(position, count, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> lpop(K key) {
        this.tx.enqueue(this.reactive::decodeV);
        return this.reactive._lpop(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> lpop(K key, int count) {
        this.tx.enqueue(this.reactive::decodeListV);
        return this.reactive._lpop(key, count).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> lpos(K key, V element) {
        this.tx.enqueue(this.reactive::decodeLongOrNull);
        return this.reactive._lpos(key, element).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> lpos(K key, V element, LPosArgs args) {
        this.tx.enqueue(this.reactive::decodeLongOrNull);
        return this.reactive._lpos(key, element, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> lpos(K key, V element, int count) {
        this.tx.enqueue(this.reactive::decodeListOfLongs);
        return this.reactive._lpos(key, element, count).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> lpos(K key, V element, int count, LPosArgs args) {
        this.tx.enqueue(this.reactive::decodeListOfLongs);
        return this.reactive._lpos(key, element, count, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> lpush(K key, V... elements) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._lpush(key, elements).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> lpushx(K key, V... elements) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._lpushx(key, elements).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> lrange(K key, long start, long stop) {
        this.tx.enqueue(this.reactive::decodeListV);
        return this.reactive._lrange(key, start, stop).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> lrem(K key, long count, V element) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._lrem(key, count, element).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> lset(K key, long index, V element) {
        this.tx.enqueue(resp -> null);
        return this.reactive._lset(key, index, element).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> ltrim(K key, long start, long stop) {
        this.tx.enqueue(resp -> null);
        return this.reactive._ltrim(key, start, stop).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> rpop(K key) {
        this.tx.enqueue(this.reactive::decodeV);
        return this.reactive._rpop(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> rpop(K key, int count) {
        this.tx.enqueue(this.reactive::decodeListV);
        return this.reactive._rpop(key, count).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Deprecated
    @Override
    public Uni<Void> rpoplpush(K source, K destination) {
        this.tx.enqueue(this.reactive::decodeV);
        return this.reactive._rpoplpush(source, destination).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> rpush(K key, V... values) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._rpush(key, values).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> rpushx(K key, V... values) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._rpushx(key, values).invoke(this::queuedOrDiscard).replaceWithVoid();
    }
}
