package io.quarkus.redis.runtime.datasource;

import java.time.Duration;

import io.quarkus.redis.datasource.list.LPosArgs;
import io.quarkus.redis.datasource.list.Position;
import io.quarkus.redis.datasource.list.ReactiveTransactionalListCommands;
import io.quarkus.redis.datasource.list.TransactionalListCommands;
import io.quarkus.redis.datasource.transactions.TransactionalRedisDataSource;

public class BlockingTransactionalListCommandsImpl<K, V> extends AbstractTransactionalRedisCommandGroup
        implements TransactionalListCommands<K, V> {

    private final ReactiveTransactionalListCommands<K, V> reactive;

    public BlockingTransactionalListCommandsImpl(TransactionalRedisDataSource ds,
            ReactiveTransactionalListCommands<K, V> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public void blmove(K source, K destination, Position positionInSource, Position positionInDest, Duration timeout) {
        this.reactive.blmove(source, destination, positionInSource, positionInDest, timeout).await()
                .atMost(this.timeout);
    }

    @Override
    public void blmpop(Duration timeout, Position position, K... keys) {
        this.reactive.blmpop(timeout, position, keys).await().atMost(this.timeout);
    }

    @Override
    public void blmpop(Duration timeout, Position position, int count, K... keys) {
        this.reactive.blmpop(timeout, position, count, keys).await().atMost(this.timeout);
    }

    @Override
    public void blpop(Duration timeout, K... keys) {
        this.reactive.blpop(timeout, keys).await().atMost(this.timeout);
    }

    @Override
    public void brpop(Duration timeout, K... keys) {
        this.reactive.brpop(timeout, keys).await().atMost(this.timeout);
    }

    @Deprecated
    @Override
    public void brpoplpush(Duration timeout, K source, K destination) {
        this.reactive.brpoplpush(timeout, source, destination).await().atMost(this.timeout);
    }

    @Override
    public void lindex(K key, long index) {
        this.reactive.lindex(key, index).await().atMost(this.timeout);
    }

    @Override
    public void linsertBeforePivot(K key, V pivot, V element) {
        this.reactive.linsertBeforePivot(key, pivot, element).await().atMost(this.timeout);
    }

    @Override
    public void linsertAfterPivot(K key, V pivot, V element) {
        this.reactive.linsertAfterPivot(key, pivot, element).await().atMost(this.timeout);
    }

    @Override
    public void llen(K key) {
        this.reactive.llen(key).await().atMost(this.timeout);
    }

    @Override
    public void lmove(K source, K destination, Position positionInSource, Position positionInDestination) {
        this.reactive.lmove(source, destination, positionInSource, positionInDestination).await().atMost(this.timeout);
    }

    @Override
    public void lmpop(Position position, K... keys) {
        this.reactive.lmpop(position, keys).await().atMost(this.timeout);
    }

    @Override
    public void lmpop(Position position, int count, K... keys) {
        this.reactive.lmpop(position, count, keys).await().atMost(this.timeout);
    }

    @Override
    public void lpop(K key) {
        this.reactive.lpop(key).await().atMost(this.timeout);
    }

    @Override
    public void lpop(K key, int count) {
        this.reactive.lpop(key, count).await().atMost(this.timeout);
    }

    @Override
    public void lpos(K key, V element) {
        this.reactive.lpos(key, element).await().atMost(this.timeout);
    }

    @Override
    public void lpos(K key, V element, LPosArgs args) {
        this.reactive.lpos(key, element, args).await().atMost(this.timeout);
    }

    @Override
    public void lpos(K key, V element, int count) {
        this.reactive.lpos(key, element, count).await().atMost(this.timeout);
    }

    @Override
    public void lpos(K key, V element, int count, LPosArgs args) {
        this.reactive.lpos(key, element, count, args).await().atMost(this.timeout);
    }

    @Override
    public void lpush(K key, V... elements) {
        this.reactive.lpush(key, elements).await().atMost(this.timeout);
    }

    @Override
    public void lpushx(K key, V... elements) {
        this.reactive.lpushx(key, elements).await().atMost(this.timeout);
    }

    @Override
    public void lrange(K key, long start, long stop) {
        this.reactive.lrange(key, start, stop).await().atMost(this.timeout);
    }

    @Override
    public void lrem(K key, long count, V element) {
        this.reactive.lrem(key, count, element).await().atMost(this.timeout);
    }

    @Override
    public void lset(K key, long index, V element) {
        this.reactive.lset(key, index, element).await().atMost(this.timeout);
    }

    @Override
    public void ltrim(K key, long start, long stop) {
        this.reactive.ltrim(key, start, stop).await().atMost(this.timeout);
    }

    @Override
    public void rpop(K key) {
        this.reactive.rpop(key).await().atMost(this.timeout);
    }

    @Override
    public void rpop(K key, int count) {
        this.reactive.rpop(key, count).await().atMost(this.timeout);
    }

    @Deprecated
    @Override
    public void rpoplpush(K source, K destination) {
        this.reactive.rpoplpush(source, destination).await().atMost(this.timeout);
    }

    @Override
    public void rpush(K key, V... values) {
        this.reactive.rpush(key, values).await().atMost(this.timeout);
    }

    @Override
    public void rpushx(K key, V... values) {
        this.reactive.rpushx(key, values).await().atMost(this.timeout);
    }
}
