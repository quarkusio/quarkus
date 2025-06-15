package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.util.List;
import java.util.OptionalLong;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.SortArgs;
import io.quarkus.redis.datasource.list.KeyValue;
import io.quarkus.redis.datasource.list.LPosArgs;
import io.quarkus.redis.datasource.list.ListCommands;
import io.quarkus.redis.datasource.list.Position;
import io.quarkus.redis.datasource.list.ReactiveListCommands;

public class BlockingListCommandsImpl<K, V> extends AbstractRedisCommandGroup implements ListCommands<K, V> {

    private final ReactiveListCommands<K, V> reactive;

    public BlockingListCommandsImpl(RedisDataSource ds, ReactiveListCommands<K, V> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public V blmove(K source, K destination, Position positionInSource, Position positionInDest, Duration timeout) {
        return reactive.blmove(source, destination, positionInSource, positionInDest, timeout).await()
                .atMost(this.timeout);
    }

    @Override
    public KeyValue<K, V> blmpop(Duration timeout, Position position, K... keys) {
        return reactive.blmpop(timeout, position, keys).await().atMost(this.timeout);
    }

    @Override
    public List<KeyValue<K, V>> blmpop(Duration timeout, Position position, int count, K... keys) {
        return reactive.blmpop(timeout, position, count, keys).await().atMost(this.timeout);
    }

    @Override
    public KeyValue<K, V> blpop(Duration timeout, K... keys) {
        return reactive.blpop(timeout, keys).await().atMost(this.timeout);
    }

    @Override
    public KeyValue<K, V> brpop(Duration timeout, K... keys) {
        return reactive.brpop(timeout, keys).await().atMost(this.timeout);
    }

    @Override
    public V brpoplpush(Duration timeout, K source, K destination) {
        return reactive.brpoplpush(timeout, source, destination).await().atMost(this.timeout);
    }

    @Override
    public V lindex(K key, long index) {
        return reactive.lindex(key, index).await().atMost(timeout);
    }

    @Override
    public long linsertBeforePivot(K key, V pivot, V element) {
        return reactive.linsertBeforePivot(key, pivot, element).await().atMost(timeout);
    }

    @Override
    public long linsertAfterPivot(K key, V pivot, V element) {
        return reactive.linsertAfterPivot(key, pivot, element).await().atMost(timeout);
    }

    @Override
    public long llen(K key) {
        return reactive.llen(key).await().atMost(timeout);
    }

    @Override
    public V lmove(K source, K destination, Position positionInSource, Position positionInDestination) {
        return reactive.lmove(source, destination, positionInSource, positionInDestination).await().atMost(timeout);
    }

    @Override
    public KeyValue<K, V> lmpop(Position position, K... keys) {
        return reactive.lmpop(position, keys).await().atMost(timeout);
    }

    @Override
    public List<KeyValue<K, V>> lmpop(Position position, int count, K... keys) {
        return reactive.lmpop(position, count, keys).await().atMost(timeout);
    }

    @Override
    public V lpop(K key) {
        return reactive.lpop(key).await().atMost(timeout);
    }

    @Override
    public List<V> lpop(K key, int count) {
        return reactive.lpop(key, count).await().atMost(timeout);
    }

    @Override
    public OptionalLong lpos(K key, V element) {
        return reactive.lpos(key, element).map(l -> {
            if (l == null) {
                return OptionalLong.empty();
            }
            return OptionalLong.of(l);
        }).await().atMost(timeout);
    }

    @Override
    public OptionalLong lpos(K key, V element, LPosArgs args) {
        return reactive.lpos(key, element, args).map(l -> {
            if (l == null) {
                return OptionalLong.empty();
            }
            return OptionalLong.of(l);
        }).await().atMost(timeout);
    }

    @Override
    public List<Long> lpos(K key, V element, int count) {
        return reactive.lpos(key, element, count).await().atMost(timeout);
    }

    @Override
    public List<Long> lpos(K key, V element, int count, LPosArgs args) {
        return reactive.lpos(key, element, count, args).await().atMost(timeout);
    }

    @Override
    public long lpush(K key, V... elements) {
        return reactive.lpush(key, elements).await().atMost(timeout);
    }

    @Override
    public long lpushx(K key, V... elements) {
        return reactive.lpushx(key, elements).await().atMost(timeout);
    }

    @Override
    public List<V> lrange(K key, long start, long stop) {
        return reactive.lrange(key, start, stop).await().atMost(timeout);
    }

    @Override
    public long lrem(K key, long count, V element) {
        return reactive.lrem(key, count, element).await().atMost(timeout);
    }

    @Override
    public void lset(K key, long index, V element) {
        reactive.lset(key, index, element).await().atMost(timeout);
    }

    @Override
    public void ltrim(K key, long start, long stop) {
        reactive.ltrim(key, start, stop).await().atMost(timeout);
    }

    @Override
    public V rpop(K key) {
        return reactive.rpop(key).await().atMost(timeout);
    }

    @Override
    public List<V> rpop(K key, int count) {
        return reactive.rpop(key, count).await().atMost(timeout);
    }

    @Override
    public V rpoplpush(K source, K destination) {
        return reactive.rpoplpush(source, destination).await().atMost(timeout);
    }

    @Override
    public long rpush(K key, V... values) {
        return reactive.rpush(key, values).await().atMost(timeout);
    }

    @Override
    public long rpushx(K key, V... values) {
        return reactive.rpushx(key, values).await().atMost(timeout);
    }

    @Override
    public List<V> sort(K key) {
        return reactive.sort(key).await().atMost(timeout);
    }

    @Override
    public List<V> sort(K key, SortArgs sortArguments) {
        return reactive.sort(key, sortArguments).await().atMost(timeout);
    }

    @Override
    public long sortAndStore(K key, K destination, SortArgs sortArguments) {
        return reactive.sortAndStore(key, destination, sortArguments).await().atMost(timeout);
    }

    @Override
    public long sortAndStore(K key, K destination) {
        return reactive.sortAndStore(key, destination).await().atMost(timeout);
    }
}
