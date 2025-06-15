package io.quarkus.redis.runtime.datasource;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.List;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.list.KeyValue;
import io.quarkus.redis.datasource.list.LPosArgs;
import io.quarkus.redis.datasource.list.Position;
import io.quarkus.redis.datasource.list.ReactiveListCommands;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveListCommandsImpl<K, V> extends AbstractListCommands<K, V> implements ReactiveListCommands<K, V> {

    private final ReactiveRedisDataSource reactive;

    public ReactiveListCommandsImpl(ReactiveRedisDataSourceImpl redis, Type k, Type v) {
        super(redis, k, v);
        this.reactive = redis;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return reactive;
    }

    @Override
    public Uni<V> blmove(K source, K destination, Position positionInSource, Position positionInDest,
            Duration timeout) {
        return super._blmove(source, destination, positionInSource, positionInDest, timeout).map(this::decodeV);
    }

    @Override
    public Uni<KeyValue<K, V>> blmpop(Duration timeout, Position position, K... keys) {
        return super._blmpop(timeout, position, keys).map(this::decodeKeyValueWithList);
    }

    @Override
    public Uni<List<KeyValue<K, V>>> blmpop(Duration timeout, Position position, int count, K... keys) {
        return super._blmpop(timeout, position, count, keys).map(this::decodeListOfKeyValue);
    }

    @Override
    public Uni<KeyValue<K, V>> blpop(Duration timeout, K... keys) {
        return super._blpop(timeout, keys).map(this::decodeKeyValue);
    }

    @Override
    public Uni<KeyValue<K, V>> brpop(Duration timeout, K... keys) {
        return super._brpop(timeout, keys).map(this::decodeKeyValue);
    }

    @Override
    public Uni<V> brpoplpush(Duration timeout, K source, K destination) {
        return super._brpoplpush(timeout, source, destination).map(this::decodeV);
    }

    @Override
    public Uni<V> lindex(K key, long index) {
        return super._lindex(key, index).map(this::decodeV);
    }

    @Override
    public Uni<Long> linsertBeforePivot(K key, V pivot, V element) {
        return super._linsertBeforePivot(key, pivot, element).map(Response::toLong);
    }

    @Override
    public Uni<Long> linsertAfterPivot(K key, V pivot, V element) {
        return super._linsertAfterPivot(key, pivot, element).map(Response::toLong);
    }

    @Override
    public Uni<Long> llen(K key) {
        return super._llen(key).map(Response::toLong);
    }

    @Override
    public Uni<V> lmove(K source, K destination, Position positionInSource, Position positionInDest) {
        return super._lmove(source, destination, positionInSource, positionInDest).map(this::decodeV);
    }

    @Override
    public Uni<KeyValue<K, V>> lmpop(Position position, K... keys) {
        return super._lmpop(position, keys).map(this::decodeKeyValueWithList);
    }

    @Override
    public Uni<List<KeyValue<K, V>>> lmpop(Position position, int count, K... keys) {
        return super._lmpop(position, count, keys).map(this::decodeListOfKeyValue);
    }

    @Override
    public Uni<V> lpop(K key) {
        return super._lpop(key).map(this::decodeV);
    }

    @Override
    public Uni<List<V>> lpop(K key, int count) {
        return super._lpop(key, count).map(this::decodeListV);
    }

    @Override
    public Uni<Long> lpos(K key, V element) {
        return lpos(key, element, DEFAULT_INSTANCE);
    }

    @Override
    public Uni<Long> lpos(K key, V element, LPosArgs args) {
        return super._lpos(key, element, args).map(this::decodeLongOrNull);
    }

    @Override
    public Uni<List<Long>> lpos(K key, V element, int count) {
        return lpos(key, element, count, DEFAULT_INSTANCE);
    }

    @Override
    public Uni<List<Long>> lpos(K key, V element, int count, LPosArgs args) {
        return super._lpos(key, element, count, args).map(this::decodeListOfLongs);
    }

    @Override
    public Uni<Long> lpush(K key, V... elements) {
        return super._lpush(key, elements).map(Response::toLong);

    }

    @Override
    public Uni<Long> lpushx(K key, V... elements) {
        return super._lpushx(key, elements).map(Response::toLong);
    }

    @Override
    public Uni<List<V>> lrange(K key, long start, long stop) {
        return super._lrange(key, start, stop).map(this::decodeListV);
    }

    @Override
    public Uni<Long> lrem(K key, long count, V element) {
        return super._lrem(key, count, element).map(Response::toLong);

    }

    @Override
    public Uni<Void> lset(K key, long index, V element) {
        return super._lset(key, index, element).replaceWithVoid();
    }

    @Override
    public Uni<Void> ltrim(K key, long start, long stop) {
        return super._ltrim(key, start, stop).replaceWithVoid();
    }

    @Override
    public Uni<V> rpop(K key) {
        return super._rpop(key).map(this::decodeV);
    }

    @Override
    public Uni<List<V>> rpop(K key, int count) {
        return super._rpop(key, count).map(this::decodeListV);
    }

    @Override
    public Uni<V> rpoplpush(K source, K destination) {
        return super._rpoplpush(source, destination).map(this::decodeV);
    }

    @Override
    public Uni<Long> rpush(K key, V... values) {
        return super._rpush(key, values).map(Response::toLong);
    }

    @Override
    public Uni<Long> rpushx(K key, V... values) {
        return super._rpushx(key, values).map(Response::toLong);
    }
}
