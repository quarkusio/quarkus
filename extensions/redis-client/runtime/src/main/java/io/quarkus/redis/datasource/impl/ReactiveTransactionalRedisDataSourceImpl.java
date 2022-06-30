package io.quarkus.redis.datasource.impl;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.Arrays;

import io.quarkus.redis.datasource.api.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.api.bitmap.ReactiveTransactionalBitMapCommands;
import io.quarkus.redis.datasource.api.geo.ReactiveTransactionalGeoCommands;
import io.quarkus.redis.datasource.api.hash.ReactiveTransactionalHashCommands;
import io.quarkus.redis.datasource.api.hyperloglog.ReactiveTransactionalHyperLogLogCommands;
import io.quarkus.redis.datasource.api.keys.ReactiveTransactionalKeyCommands;
import io.quarkus.redis.datasource.api.list.ReactiveTransactionalListCommands;
import io.quarkus.redis.datasource.api.set.ReactiveTransactionalSetCommands;
import io.quarkus.redis.datasource.api.sortedset.ReactiveTransactionalSortedSetCommands;
import io.quarkus.redis.datasource.api.string.ReactiveTransactionalStringCommands;
import io.quarkus.redis.datasource.api.transactions.ReactiveTransactionalRedisDataSource;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;

public class ReactiveTransactionalRedisDataSourceImpl implements ReactiveTransactionalRedisDataSource {

    private final ReactiveRedisDataSourceImpl reactive;
    private final TransactionHolder tx;

    public ReactiveTransactionalRedisDataSourceImpl(ReactiveRedisDataSource source, TransactionHolder th) {
        this.tx = th;
        this.reactive = (ReactiveRedisDataSourceImpl) source;
    }

    @Override
    public Uni<Void> discard() {
        return reactive.execute(Command.DISCARD)
                .invoke(tx::discard)
                .replaceWithVoid();
    }

    @Override
    public boolean discarded() {
        return tx.discarded();
    }

    @Override
    public <K, F, V> ReactiveTransactionalHashCommands<K, F, V> hash(Class<K> redisKeyType, Class<F> typeOfField,
            Class<V> typeOfValue) {
        return new ReactiveTransactionalHashCommandsImpl<>(
                (ReactiveHashCommandsImpl<K, F, V>) this.reactive.hash(redisKeyType, typeOfField, typeOfValue),
                tx);
    }

    @Override
    public <K, V> ReactiveTransactionalGeoCommands<K, V> geo(Class<K> redisKeyType, Class<V> memberType) {
        return new ReactiveTransactionalGeoCommandsImpl<>(
                (ReactiveGeoCommandsImpl<K, V>) this.reactive.geo(redisKeyType, memberType), tx);
    }

    @Override
    public <K, V> ReactiveTransactionalSortedSetCommands<K, V> sortedSet(Class<K> redisKeyType, Class<V> valueType) {
        return new ReactiveTransactionalSortedSetCommandsImpl<>(
                (ReactiveSortedSetCommandsImpl<K, V>) this.reactive.sortedSet(redisKeyType, valueType), tx);
    }

    @Override
    public <K, V> ReactiveTransactionalStringCommands<K, V> string(Class<K> redisKeyType, Class<V> valueType) {
        return new ReactiveTransactionalStringCommandsImpl<>(
                (ReactiveStringCommandsImpl<K, V>) this.reactive.string(redisKeyType, valueType), tx);
    }

    @Override
    public <K, V> ReactiveTransactionalSetCommands<K, V> set(Class<K> redisKeyType, Class<V> memberType) {
        return new ReactiveTransactionalSetCommandsImpl<>(
                (ReactiveSetCommandsImpl<K, V>) this.reactive.set(redisKeyType, memberType), tx);
    }

    @Override
    public <K, V> ReactiveTransactionalListCommands<K, V> list(Class<K> redisKeyType, Class<V> memberType) {
        return new ReactiveTransactionalListCommandsImpl<>(
                (ReactiveListCommandsImpl<K, V>) this.reactive.list(redisKeyType, memberType), tx);
    }

    @Override
    public <K, V> ReactiveTransactionalHyperLogLogCommands<K, V> hyperloglog(Class<K> redisKeyType, Class<V> memberType) {
        return new ReactiveTransactionalHyperLogLogCommandsImpl<>(
                (ReactiveHyperLogLogCommandsImpl<K, V>) this.reactive.hyperloglog(redisKeyType, memberType), tx);
    }

    @Override
    public <K> ReactiveTransactionalBitMapCommands<K> bitmap(Class<K> redisKeyType) {
        return new ReactiveTransactionalBitMapCommandsImpl<>(
                (ReactiveBitMapCommandsImpl<K>) this.reactive.bitmap(redisKeyType), tx);
    }

    @Override
    public <K> ReactiveTransactionalKeyCommands<K> key(Class<K> redisKeyType) {
        return new ReactiveTransactionalKeyCommandsImpl<>(
                (ReactiveKeyCommandsImpl<K>) this.reactive.key(redisKeyType), tx);
    }

    @Override
    public Uni<Void> execute(String command, String... args) {
        nonNull(command, "command");
        return execute(Command.create(command), args);
    }

    @Override
    public Uni<Void> execute(Command command, String... args) {
        nonNull(command, "command");
        tx.enqueue(r -> r); // identity

        RedisCommand c = RedisCommand.of(command).putAll(Arrays.asList(args));

        return reactive.execute(c.toRequest())
                .map(r -> {
                    if (r == null || !r.toString().equals("QUEUED")) {
                        this.tx.discard();
                        throw new IllegalStateException("Unable to enqueue command into the current transaction");
                    }
                    return r;
                })
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> execute(io.vertx.redis.client.Command command, String... args) {
        nonNull(command, "command");
        return execute(new Command(command), args);
    }
}
