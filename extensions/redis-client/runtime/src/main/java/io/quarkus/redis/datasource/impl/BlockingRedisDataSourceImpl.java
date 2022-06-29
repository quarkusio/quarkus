package io.quarkus.redis.datasource.impl;

import static io.quarkus.redis.datasource.impl.ReactiveRedisDataSourceImpl.toTransactionResult;

import java.time.Duration;
import java.util.function.Consumer;

import io.quarkus.redis.datasource.api.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.api.RedisDataSource;
import io.quarkus.redis.datasource.api.bitmap.BitMapCommands;
import io.quarkus.redis.datasource.api.geo.GeoCommands;
import io.quarkus.redis.datasource.api.hash.HashCommands;
import io.quarkus.redis.datasource.api.hyperloglog.HyperLogLogCommands;
import io.quarkus.redis.datasource.api.keys.KeyCommands;
import io.quarkus.redis.datasource.api.list.ListCommands;
import io.quarkus.redis.datasource.api.pubsub.PubSubCommands;
import io.quarkus.redis.datasource.api.set.SetCommands;
import io.quarkus.redis.datasource.api.sortedset.SortedSetCommands;
import io.quarkus.redis.datasource.api.string.StringCommands;
import io.quarkus.redis.datasource.api.transactions.TransactionResult;
import io.quarkus.redis.datasource.api.transactions.TransactionalRedisDataSource;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.mutiny.redis.client.RedisConnection;
import io.vertx.mutiny.redis.client.Request;
import io.vertx.mutiny.redis.client.Response;

public class BlockingRedisDataSourceImpl implements RedisDataSource {

    private final Duration timeout;
    final ReactiveRedisDataSourceImpl reactive;
    final RedisConnection connection;

    public BlockingRedisDataSourceImpl(Redis redis, RedisAPI api, Duration timeout) {
        this(new ReactiveRedisDataSourceImpl(redis, api), timeout);
    }

    public BlockingRedisDataSourceImpl(ReactiveRedisDataSourceImpl reactive, Duration timeout) {
        this.reactive = reactive;
        this.timeout = timeout;
        this.connection = reactive.connection;
    }

    public BlockingRedisDataSourceImpl(Redis redis, RedisConnection connection, Duration timeout) {
        this(new ReactiveRedisDataSourceImpl(redis, connection), timeout);
    }

    public TransactionResult withTransaction(Consumer<TransactionalRedisDataSource> ds) {
        RedisConnection connection = reactive.redis.connect().await().atMost(timeout);
        ReactiveRedisDataSourceImpl dataSource = new ReactiveRedisDataSourceImpl(reactive.redis, connection);
        TransactionHolder th = new TransactionHolder();
        BlockingTransactionalRedisDataSourceImpl source = new BlockingTransactionalRedisDataSourceImpl(
                new ReactiveTransactionalRedisDataSourceImpl(dataSource, th), timeout);

        try {
            connection.send(Request.cmd(Command.MULTI)).await().atMost(timeout);
            ds.accept(source);
            if (!source.discarded()) {
                Response response = connection.send((Request.cmd(Command.EXEC))).await().atMost(timeout);
                return toTransactionResult(response, th);
            } else {
                return toTransactionResult(null, th);
            }

        } finally {
            connection.closeAndAwait();
        }
    }

    @Override
    public TransactionResult withTransaction(Consumer<TransactionalRedisDataSource> ds, String... watchedKeys) {
        RedisConnection connection = reactive.redis.connect().await().atMost(timeout);
        ReactiveRedisDataSourceImpl dataSource = new ReactiveRedisDataSourceImpl(reactive.redis, connection);
        TransactionHolder th = new TransactionHolder();
        BlockingTransactionalRedisDataSourceImpl source = new BlockingTransactionalRedisDataSourceImpl(
                new ReactiveTransactionalRedisDataSourceImpl(dataSource, th), timeout);

        try {
            Request cmd = Request.cmd(Command.WATCH);
            for (String watchedKey : watchedKeys) {
                cmd.arg(watchedKey);
            }
            connection.send(cmd).await().atMost(timeout);
            connection.send(Request.cmd(Command.MULTI)).await().atMost(timeout);

            ds.accept(source);
            if (!source.discarded()) {
                Response response = connection.send(Request.cmd(Command.EXEC)).await().atMost(timeout);
                // exec produce null is the transaction has been discarded
                return toTransactionResult(response, th);
            } else {
                return toTransactionResult(null, th);
            }

        } finally {
            connection.closeAndAwait();
        }
    }

    @Override
    public void withConnection(Consumer<RedisDataSource> consumer) {
        if (connection != null) {
            // Already on a specific connection, we keep using it
            consumer.accept(this);
            return;
        }

        BlockingRedisDataSourceImpl source = reactive.redis.connect()
                .map(rc -> new BlockingRedisDataSourceImpl(reactive.redis, rc, timeout))
                .await().atMost(timeout);

        try {
            consumer.accept(source);
        } finally {
            source.connection.closeAndAwait();
        }
    }

    @Override
    public void select(long index) {
        reactive.select(index)
                .await().atMost(timeout);
    }

    @Override
    public void flushall() {
        reactive.flushall()
                .await().atMost(timeout);
    }

    @Override
    public <K1, F, V1> HashCommands<K1, F, V1> hash(Class<K1> redisKeyType, Class<F> typeOfField, Class<V1> typeOfValue) {
        return new BlockingHashCommandsImpl<>(reactive.hash(redisKeyType, typeOfField, typeOfValue), timeout);
    }

    @Override
    public <K1, V1> GeoCommands<K1, V1> geo(Class<K1> redisKeyType, Class<V1> memberType) {
        return new BlockingGeoCommandsImpl<>(reactive.geo(redisKeyType, memberType), timeout);
    }

    @Override
    public <K1> KeyCommands<K1> key(Class<K1> redisKeyType) {
        return new BlockingKeyCommandsImpl<>(reactive.key(redisKeyType), timeout);
    }

    @Override
    public <K1, V1> SortedSetCommands<K1, V1> sortedSet(Class<K1> redisKeyType, Class<V1> valueType) {
        return new BlockingSortedSetCommandsImpl<>(reactive.sortedSet(redisKeyType, valueType), timeout);
    }

    @Override
    public <K1, V1> StringCommands<K1, V1> string(Class<K1> redisKeyType, Class<V1> valueType) {
        return new BlockingStringCommandsImpl<>(reactive.string(redisKeyType, valueType), timeout);
    }

    @Override
    public <K1, V1> SetCommands<K1, V1> set(Class<K1> redisKeyType, Class<V1> memberType) {
        return new BlockingSetCommandsImpl<>(reactive.set(redisKeyType, memberType), timeout);
    }

    @Override
    public <K1, V1> ListCommands<K1, V1> list(Class<K1> redisKeyType, Class<V1> memberType) {
        return new BlockingListCommandsImpl<>(reactive.list(redisKeyType, memberType), timeout);
    }

    @Override
    public <K1, V1> HyperLogLogCommands<K1, V1> hyperloglog(Class<K1> redisKeyType, Class<V1> memberType) {
        return new BlockingHyperLogLogCommandsImpl<>(reactive.hyperloglog(redisKeyType, memberType), timeout);
    }

    @Override
    public <K> BitMapCommands<K> bitmap(Class<K> redisKeyType) {
        return new BlockingBitmapCommandsImpl<>(reactive.bitmap(redisKeyType), timeout);
    }

    @Override
    public <V> PubSubCommands<V> pubsub(Class<V> messageType) {
        return new BlockingPubSubCommandsImpl<>(reactive.pubsub(messageType), timeout);
    }

    @Override
    public Response execute(String command, String... args) {
        return reactive.execute(command, args)
                .await().atMost(timeout);
    }

    @Override
    public Response execute(Command command, String... args) {
        return reactive.execute(command, args)
                .await().atMost(timeout);
    }

    @Override
    public Response execute(io.vertx.redis.client.Command command, String... args) {
        return reactive.execute(command, args)
                .await().atMost(timeout);
    }

    @Override
    public ReactiveRedisDataSource getReactive() {
        return reactive;
    }
}
