package io.quarkus.redis.runtime.datasource;

import static io.quarkus.redis.runtime.datasource.ReactiveRedisDataSourceImpl.toTransactionResult;

import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.autosuggest.AutoSuggestCommands;
import io.quarkus.redis.datasource.bitmap.BitMapCommands;
import io.quarkus.redis.datasource.bloom.BloomCommands;
import io.quarkus.redis.datasource.countmin.CountMinCommands;
import io.quarkus.redis.datasource.cuckoo.CuckooCommands;
import io.quarkus.redis.datasource.geo.GeoCommands;
import io.quarkus.redis.datasource.graph.GraphCommands;
import io.quarkus.redis.datasource.hash.HashCommands;
import io.quarkus.redis.datasource.hyperloglog.HyperLogLogCommands;
import io.quarkus.redis.datasource.json.JsonCommands;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.list.ListCommands;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.redis.datasource.search.SearchCommands;
import io.quarkus.redis.datasource.set.SetCommands;
import io.quarkus.redis.datasource.sortedset.SortedSetCommands;
import io.quarkus.redis.datasource.stream.StreamCommands;
import io.quarkus.redis.datasource.string.StringCommands;
import io.quarkus.redis.datasource.timeseries.TimeSeriesCommands;
import io.quarkus.redis.datasource.topk.TopKCommands;
import io.quarkus.redis.datasource.transactions.OptimisticLockingTransactionResult;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.datasource.transactions.TransactionalRedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.vertx.mutiny.core.Vertx;
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

    public BlockingRedisDataSourceImpl(Vertx vertx, Redis redis, RedisAPI api, Duration timeout) {
        this(new ReactiveRedisDataSourceImpl(vertx, redis, api), timeout);
    }

    public BlockingRedisDataSourceImpl(ReactiveRedisDataSourceImpl reactive, Duration timeout) {
        this.reactive = reactive;
        this.timeout = timeout;
        this.connection = reactive.connection;
    }

    public BlockingRedisDataSourceImpl(Vertx vertx, Redis redis, RedisConnection connection, Duration timeout) {
        this(new ReactiveRedisDataSourceImpl(vertx, redis, connection), timeout);
    }

    @Override
    public TransactionResult withTransaction(Consumer<TransactionalRedisDataSource> ds) {
        RedisConnection connection = reactive.redis.connect().await().atMost(timeout);
        ReactiveRedisDataSourceImpl dataSource = new ReactiveRedisDataSourceImpl(reactive.getVertx(), reactive.redis,
                connection);
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
        ReactiveRedisDataSourceImpl dataSource = new ReactiveRedisDataSourceImpl(reactive.getVertx(), reactive.redis,
                connection);
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
    public <I> OptimisticLockingTransactionResult<I> withTransaction(Function<RedisDataSource, I> preTxBlock,
            BiConsumer<I, TransactionalRedisDataSource> tx, String... watchedKeys) {
        RedisConnection connection = reactive.redis.connect().await().atMost(timeout);
        ReactiveRedisDataSourceImpl dataSource = new ReactiveRedisDataSourceImpl(reactive.getVertx(), reactive.redis,
                connection);
        TransactionHolder th = new TransactionHolder();
        BlockingTransactionalRedisDataSourceImpl source = new BlockingTransactionalRedisDataSourceImpl(
                new ReactiveTransactionalRedisDataSourceImpl(dataSource, th), timeout);

        try {
            Request cmd = Request.cmd(Command.WATCH);
            for (String watchedKey : watchedKeys) {
                cmd.arg(watchedKey);
            }
            connection.send(cmd).await().atMost(timeout);

            I input = preTxBlock
                    .apply(new BlockingRedisDataSourceImpl(reactive.getVertx(), reactive.redis, connection, timeout));

            connection.send(Request.cmd(Command.MULTI)).await().atMost(timeout);

            tx.accept(input, source);
            if (!source.discarded()) {
                Response response = connection.send(Request.cmd(Command.EXEC)).await().atMost(timeout);
                // exec produce null is the transaction has been discarded
                return toTransactionResult(response, input, th);
            } else {
                return toTransactionResult(null, input, th);
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
                .map(rc -> new BlockingRedisDataSourceImpl(reactive.getVertx(), reactive.redis, rc, timeout))
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
        return new BlockingHashCommandsImpl<>(this, reactive.hash(redisKeyType, typeOfField, typeOfValue), timeout);
    }

    @Override
    public <K, F, V> HashCommands<K, F, V> hash(TypeReference<K> redisKeyType, TypeReference<F> typeOfField,
            TypeReference<V> typeOfValue) {
        return new BlockingHashCommandsImpl<>(this, reactive.hash(redisKeyType, typeOfField, typeOfValue), timeout);
    }

    @Override
    public <K1, V1> GeoCommands<K1, V1> geo(Class<K1> redisKeyType, Class<V1> memberType) {
        return new BlockingGeoCommandsImpl<>(this, reactive.geo(redisKeyType, memberType), timeout);
    }

    @Override
    public <K1, V1> GeoCommands<K1, V1> geo(TypeReference<K1> redisKeyType, TypeReference<V1> memberType) {
        return new BlockingGeoCommandsImpl<>(this, reactive.geo(redisKeyType, memberType), timeout);
    }

    @Override
    public <K1> KeyCommands<K1> key(Class<K1> redisKeyType) {
        return new BlockingKeyCommandsImpl<>(this, reactive.key(redisKeyType), timeout);
    }

    @Override
    public <K1> KeyCommands<K1> key(TypeReference<K1> redisKeyType) {
        return new BlockingKeyCommandsImpl<>(this, reactive.key(redisKeyType), timeout);
    }

    @Override
    public <K1, V1> SortedSetCommands<K1, V1> sortedSet(Class<K1> redisKeyType, Class<V1> valueType) {
        return new BlockingSortedSetCommandsImpl<>(this, reactive.sortedSet(redisKeyType, valueType), timeout);
    }

    @Override
    public <K1, V1> SortedSetCommands<K1, V1> sortedSet(TypeReference<K1> redisKeyType, TypeReference<V1> valueType) {
        return new BlockingSortedSetCommandsImpl<>(this, reactive.sortedSet(redisKeyType, valueType), timeout);
    }

    @Override
    public <K1, V1> StringCommands<K1, V1> string(Class<K1> redisKeyType, Class<V1> valueType) {
        return new BlockingStringCommandsImpl<>(this, reactive.value(redisKeyType, valueType), timeout);
    }

    @Override
    public <K, V> ValueCommands<K, V> value(Class<K> redisKeyType, Class<V> valueType) {
        return new BlockingStringCommandsImpl<>(this, reactive.value(redisKeyType, valueType), timeout);
    }

    @Override
    public <K, V> ValueCommands<K, V> value(TypeReference<K> redisKeyType, TypeReference<V> valueType) {
        return new BlockingStringCommandsImpl<>(this, reactive.value(redisKeyType, valueType), timeout);
    }

    @Override
    public <K1, V1> SetCommands<K1, V1> set(Class<K1> redisKeyType, Class<V1> memberType) {
        return new BlockingSetCommandsImpl<>(this, reactive.set(redisKeyType, memberType), timeout);
    }

    @Override
    public <K1, V1> SetCommands<K1, V1> set(TypeReference<K1> redisKeyType, TypeReference<V1> memberType) {
        return new BlockingSetCommandsImpl<>(this, reactive.set(redisKeyType, memberType), timeout);
    }

    @Override
    public <K1, V1> ListCommands<K1, V1> list(Class<K1> redisKeyType, Class<V1> memberType) {
        return new BlockingListCommandsImpl<>(this, reactive.list(redisKeyType, memberType), timeout);
    }

    @Override
    public <K1, V1> ListCommands<K1, V1> list(TypeReference<K1> redisKeyType, TypeReference<V1> memberType) {
        return new BlockingListCommandsImpl<>(this, reactive.list(redisKeyType, memberType), timeout);
    }

    @Override
    public <K1, V1> HyperLogLogCommands<K1, V1> hyperloglog(Class<K1> redisKeyType, Class<V1> memberType) {
        return new BlockingHyperLogLogCommandsImpl<>(this, reactive.hyperloglog(redisKeyType, memberType), timeout);
    }

    @Override
    public <K1, V1> HyperLogLogCommands<K1, V1> hyperloglog(TypeReference<K1> redisKeyType, TypeReference<V1> memberType) {
        return new BlockingHyperLogLogCommandsImpl<>(this, reactive.hyperloglog(redisKeyType, memberType), timeout);
    }

    @Override
    public <K> BitMapCommands<K> bitmap(Class<K> redisKeyType) {
        return new BlockingBitmapCommandsImpl<>(this, reactive.bitmap(redisKeyType), timeout);
    }

    @Override
    public <K> BitMapCommands<K> bitmap(TypeReference<K> redisKeyType) {
        return new BlockingBitmapCommandsImpl<>(this, reactive.bitmap(redisKeyType), timeout);
    }

    @Override
    public <K, F, V> StreamCommands<K, F, V> stream(Class<K> redisKeyType, Class<F> fieldType, Class<V> valueType) {
        return new BlockingStreamCommandsImpl<>(this, reactive.stream(redisKeyType, fieldType, valueType), timeout);
    }

    @Override
    public <K, F, V> StreamCommands<K, F, V> stream(TypeReference<K> redisKeyType, TypeReference<F> fieldType,
            TypeReference<V> valueType) {
        return new BlockingStreamCommandsImpl<>(this, reactive.stream(redisKeyType, fieldType, valueType), timeout);
    }

    @Override
    public <K> JsonCommands<K> json(Class<K> redisKeyType) {
        return new BlockingJsonCommandsImpl<>(this, reactive.json(redisKeyType), timeout);
    }

    @Override
    public <K> JsonCommands<K> json(TypeReference<K> redisKeyType) {
        return new BlockingJsonCommandsImpl<>(this, reactive.json(redisKeyType), timeout);
    }

    @Override
    public <K, V> BloomCommands<K, V> bloom(Class<K> redisKeyType, Class<V> valueType) {
        return new BlockingBloomCommandsImpl<>(this, reactive.bloom(redisKeyType, valueType), timeout);
    }

    @Override
    public <K, V> BloomCommands<K, V> bloom(TypeReference<K> redisKeyType, TypeReference<V> valueType) {
        return new BlockingBloomCommandsImpl<>(this, reactive.bloom(redisKeyType, valueType), timeout);
    }

    @Override
    public <K, V> CuckooCommands<K, V> cuckoo(Class<K> redisKeyType, Class<V> valueType) {
        return new BlockingCuckooCommandsImpl<>(this, reactive.cuckoo(redisKeyType, valueType), timeout);
    }

    @Override
    public <K, V> CuckooCommands<K, V> cuckoo(TypeReference<K> redisKeyType, TypeReference<V> valueType) {
        return new BlockingCuckooCommandsImpl<>(this, reactive.cuckoo(redisKeyType, valueType), timeout);
    }

    @Override
    public <K, V> CountMinCommands<K, V> countmin(Class<K> redisKeyType, Class<V> valueType) {
        return new BlockingCountMinCommandsImpl<>(this, reactive.countmin(redisKeyType, valueType), timeout);
    }

    @Override
    public <K, V> CountMinCommands<K, V> countmin(TypeReference<K> redisKeyType, TypeReference<V> valueType) {
        return new BlockingCountMinCommandsImpl<>(this, reactive.countmin(redisKeyType, valueType), timeout);
    }

    @Override
    public <K, V> TopKCommands<K, V> topk(Class<K> redisKeyType, Class<V> valueType) {
        return new BlockingTopKCommandsImpl<>(this, reactive.topk(redisKeyType, valueType), timeout);
    }

    @Override
    public <K, V> TopKCommands<K, V> topk(TypeReference<K> redisKeyType, TypeReference<V> valueType) {
        return new BlockingTopKCommandsImpl<>(this, reactive.topk(redisKeyType, valueType), timeout);
    }

    @Override
    public <K> GraphCommands<K> graph(Class<K> redisKeyType) {
        return new BlockingGraphCommandsImpl<>(this, reactive.graph(redisKeyType), timeout);
    }

    @Override
    public <K> SearchCommands<K> search(Class<K> redisKeyType) {
        return new BlockingSearchCommandsImpl<>(this, reactive.search(redisKeyType), timeout);
    }

    @Override
    public <K> AutoSuggestCommands<K> autosuggest(Class<K> redisKeyType) {
        return new BlockingAutoSuggestCommandsImpl<>(this, reactive.autosuggest(redisKeyType), timeout);
    }

    @Override
    public <K> AutoSuggestCommands<K> autosuggest(TypeReference<K> redisKeyType) {
        return new BlockingAutoSuggestCommandsImpl<>(this, reactive.autosuggest(redisKeyType), timeout);
    }

    @Override
    public <K> TimeSeriesCommands<K> timeseries(Class<K> redisKeyType) {
        return new BlockingTimeSeriesCommandsImpl<>(this, reactive.timeseries(redisKeyType), timeout);
    }

    @Override
    public <K> TimeSeriesCommands<K> timeseries(TypeReference<K> redisKeyType) {
        return new BlockingTimeSeriesCommandsImpl<>(this, reactive.timeseries(redisKeyType), timeout);
    }

    @Override
    public <V> PubSubCommands<V> pubsub(Class<V> messageType) {
        return new BlockingPubSubCommandsImpl<>(this, reactive.pubsub(messageType), timeout);
    }

    @Override
    public <V> PubSubCommands<V> pubsub(TypeReference<V> messageType) {
        return new BlockingPubSubCommandsImpl<>(this, reactive.pubsub(messageType), timeout);
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
