package io.quarkus.redis.runtime.datasource;

import java.time.Duration;

import io.quarkus.redis.datasource.bitmap.TransactionalBitMapCommands;
import io.quarkus.redis.datasource.geo.TransactionalGeoCommands;
import io.quarkus.redis.datasource.hash.TransactionalHashCommands;
import io.quarkus.redis.datasource.hyperloglog.TransactionalHyperLogLogCommands;
import io.quarkus.redis.datasource.keys.TransactionalKeyCommands;
import io.quarkus.redis.datasource.list.TransactionalListCommands;
import io.quarkus.redis.datasource.set.TransactionalSetCommands;
import io.quarkus.redis.datasource.sortedset.TransactionalSortedSetCommands;
import io.quarkus.redis.datasource.string.TransactionalStringCommands;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.quarkus.redis.datasource.transactions.TransactionalRedisDataSource;
import io.vertx.mutiny.redis.client.Command;

public class BlockingTransactionalRedisDataSourceImpl implements TransactionalRedisDataSource {

    private final ReactiveTransactionalRedisDataSource reactive;
    private final Duration timeout;

    public BlockingTransactionalRedisDataSourceImpl(ReactiveTransactionalRedisDataSource api, Duration timeout) {
        this.reactive = api;
        this.timeout = timeout;
    }

    @Override
    public void discard() {
        reactive.discard().await().atMost(timeout);
    }

    @Override
    public boolean discarded() {
        return reactive.discarded();
    }

    @Override
    public <K, F, V> TransactionalHashCommands<K, F, V> hash(Class<K> redisKeyType, Class<F> typeOfField,
            Class<V> typeOfValue) {
        return new BlockingTransactionalHashCommandsImpl<>(reactive.hash(redisKeyType, typeOfField, typeOfValue), timeout);
    }

    @Override
    public <K, V> TransactionalGeoCommands<K, V> geo(Class<K> redisKeyType, Class<V> memberType) {
        return new BlockingTransactionalGeoCommandsImpl<>(reactive.geo(redisKeyType, memberType), timeout);
    }

    @Override
    public <K> TransactionalKeyCommands<K> key(Class<K> redisKeyType) {
        return new BlockingTransactionalKeyCommandsImpl<>(reactive.key(redisKeyType), timeout);
    }

    @Override
    public <K, V> TransactionalSetCommands<K, V> set(Class<K> redisKeyType, Class<V> memberType) {
        return new BlockingTransactionalSetCommandsImpl<>(reactive.set(redisKeyType, memberType), timeout);
    }

    @Override
    public <K, V> TransactionalSortedSetCommands<K, V> sortedSet(Class<K> redisKeyType, Class<V> valueType) {
        return new BlockingTransactionalSortedSetCommandsImpl<>(reactive.sortedSet(redisKeyType, valueType), timeout);
    }

    @Override
    public <K, V> TransactionalStringCommands<K, V> string(Class<K> redisKeyType, Class<V> valueType) {
        return new BlockingTransactionalStringCommandsImpl<>(reactive.string(redisKeyType, valueType), timeout);
    }

    @Override
    public <K, V> TransactionalListCommands<K, V> list(Class<K> redisKeyType, Class<V> memberType) {
        return new BlockingTransactionalListCommandsImpl<>(reactive.list(redisKeyType, memberType), timeout);
    }

    @Override
    public <K, V> TransactionalHyperLogLogCommands<K, V> hyperloglog(Class<K> redisKeyType, Class<V> memberType) {
        return new BlockingTransactionalHyperLogLogCommandsImpl<>(reactive.hyperloglog(redisKeyType, memberType), timeout);
    }

    @Override
    public <K> TransactionalBitMapCommands<K> bitmap(Class<K> redisKeyType) {
        return new BlockingTransactionalBitMapCommandsImpl<>(reactive.bitmap(redisKeyType), timeout);
    }

    @Override
    public void execute(String command, String... args) {
        reactive.execute(command, args).await().atMost(timeout);
    }

    @Override
    public void execute(Command command, String... args) {
        reactive.execute(command, args).await().atMost(timeout);
    }

    @Override
    public void execute(io.vertx.redis.client.Command command, String... args) {
        reactive.execute(command, args).await().atMost(timeout);
    }
}
