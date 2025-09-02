package io.quarkus.redis.runtime.datasource;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positiveOrZero;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.autosuggest.ReactiveAutoSuggestCommands;
import io.quarkus.redis.datasource.bitmap.ReactiveBitMapCommands;
import io.quarkus.redis.datasource.bloom.ReactiveBloomCommands;
import io.quarkus.redis.datasource.countmin.ReactiveCountMinCommands;
import io.quarkus.redis.datasource.cuckoo.ReactiveCuckooCommands;
import io.quarkus.redis.datasource.geo.ReactiveGeoCommands;
import io.quarkus.redis.datasource.graph.ReactiveGraphCommands;
import io.quarkus.redis.datasource.hash.ReactiveHashCommands;
import io.quarkus.redis.datasource.hyperloglog.ReactiveHyperLogLogCommands;
import io.quarkus.redis.datasource.json.ReactiveJsonCommands;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.list.ReactiveListCommands;
import io.quarkus.redis.datasource.pubsub.ReactivePubSubCommands;
import io.quarkus.redis.datasource.search.ReactiveSearchCommands;
import io.quarkus.redis.datasource.set.ReactiveSetCommands;
import io.quarkus.redis.datasource.sortedset.ReactiveSortedSetCommands;
import io.quarkus.redis.datasource.stream.ReactiveStreamCommands;
import io.quarkus.redis.datasource.string.ReactiveStringCommands;
import io.quarkus.redis.datasource.timeseries.ReactiveTimeSeriesCommands;
import io.quarkus.redis.datasource.topk.ReactiveTopKCommands;
import io.quarkus.redis.datasource.transactions.OptimisticLockingTransactionResult;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.mutiny.redis.client.RedisConnection;
import io.vertx.mutiny.redis.client.Request;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveRedisDataSourceImpl implements ReactiveRedisDataSource, RedisCommandExecutor {

    final Redis redis;
    final RedisConnection connection;
    private final Vertx vertx;

    public ReactiveRedisDataSourceImpl(Vertx vertx, Redis redis, RedisAPI api) {
        nonNull(redis, "redis");
        nonNull(api, "api");
        nonNull(vertx, "vertx");
        this.vertx = vertx;
        this.redis = redis;
        this.connection = null;
    }

    public ReactiveRedisDataSourceImpl(Vertx vertx, Redis redis, RedisConnection connection) {
        nonNull(redis, "redis");
        nonNull(connection, "connection");
        nonNull(vertx, "vertx");
        this.vertx = vertx;
        this.redis = redis;
        this.connection = connection;
    }

    @Override
    public Uni<Response> execute(Request request) {
        if (connection != null) {
            return connection.send(request);
        }
        return redis.send(request);
    }

    @Override
    public Uni<TransactionResult> withTransaction(Function<ReactiveTransactionalRedisDataSource, Uni<Void>> function) {
        nonNull(function, "function");
        return redis.connect()
                .onItem().transformToUni(connection -> {
                    ReactiveRedisDataSourceImpl singleConnectionDS = new ReactiveRedisDataSourceImpl(vertx, redis, connection);
                    TransactionHolder th = new TransactionHolder();
                    return connection.send(Request.cmd(Command.MULTI))
                            .chain(x -> function.apply(new ReactiveTransactionalRedisDataSourceImpl(singleConnectionDS, th)))
                            .chain(ignored -> {
                                if (!th.discarded()) {
                                    return connection.send(Request.cmd(Command.EXEC));
                                } else {
                                    return Uni.createFrom().nullItem();
                                }
                            })
                            .onTermination().call(connection::close)
                            .map(r -> toTransactionResult(r, th));
                });
    }

    @Override
    public Uni<TransactionResult> withTransaction(Function<ReactiveTransactionalRedisDataSource, Uni<Void>> function,
            String... keys) {
        nonNull(function, "function");
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        return redis.connect()
                .onItem().transformToUni(connection -> {
                    ReactiveRedisDataSourceImpl singleConnectionDS = new ReactiveRedisDataSourceImpl(vertx, redis, connection);
                    TransactionHolder th = new TransactionHolder();
                    return watch(connection, keys) // WATCH keys
                            .chain(() -> connection.send(Request.cmd(Command.MULTI))
                                    .chain(x -> function
                                            .apply(new ReactiveTransactionalRedisDataSourceImpl(singleConnectionDS, th)))
                                    .onItemOrFailure().transformToUni((x, failure) -> {
                                        if (!th.discarded() && failure == null) {
                                            return connection.send(Request.cmd(Command.EXEC));
                                        } else {
                                            if (!th.discarded()) {
                                                return connection.send(Request.cmd(Command.DISCARD));
                                            }
                                            return Uni.createFrom().nullItem();
                                        }
                                    })
                                    .onTermination().call(connection::close)
                                    .map(r -> toTransactionResult(r, th)));
                });
    }

    private Uni<Void> watch(RedisConnection connection, String... keys) {
        List<String> watched = List.of(keys);
        Request request = Request.cmd(Command.WATCH);
        for (String s : watched) {
            request.arg(s);
        }
        return connection.send(request)
                .replaceWithVoid();
    }

    @Override
    public <I> Uni<OptimisticLockingTransactionResult<I>> withTransaction(Function<ReactiveRedisDataSource, Uni<I>> preTxBlock,
            BiFunction<I, ReactiveTransactionalRedisDataSource, Uni<Void>> tx, String... watchedKeys) {
        nonNull(tx, "tx");
        notNullOrEmpty(watchedKeys, "watchedKeys");
        doesNotContainNull(watchedKeys, "watchedKeys");
        nonNull(preTxBlock, "preTxBlock");

        return redis.connect()
                .onItem().transformToUni(connection -> {
                    ReactiveRedisDataSourceImpl singleConnectionDS = new ReactiveRedisDataSourceImpl(vertx, redis, connection);
                    TransactionHolder th = new TransactionHolder();
                    return watch(connection, watchedKeys) // WATCH keys
                            .chain(x -> preTxBlock.apply(new ReactiveRedisDataSourceImpl(vertx, redis, connection)))// Execute the pre-tx-block
                            .chain(input -> connection.send(Request.cmd(Command.MULTI))
                                    .chain(x -> tx
                                            .apply(input, new ReactiveTransactionalRedisDataSourceImpl(singleConnectionDS, th)))
                                    .onItemOrFailure().transformToUni((x, failure) -> {
                                        if (!th.discarded() && failure == null) {
                                            return connection.send(Request.cmd(Command.EXEC));
                                        } else {
                                            if (!th.discarded()) {
                                                return connection.send(Request.cmd(Command.DISCARD))
                                                        .replaceWithNull();
                                            }
                                            return Uni.createFrom().nullItem();
                                        }
                                    })
                                    .onTermination().call(connection::close)
                                    .map(r -> toTransactionResult(r, input, th)));
                });
    }

    public static TransactionResult toTransactionResult(Response response, TransactionHolder th) {
        if (response == null) {
            // Discarded
            return TransactionResultImpl.DISCARDED;
        }
        return new TransactionResultImpl(th.discarded(), th.map(response));
    }

    public static <I> OptimisticLockingTransactionResult<I> toTransactionResult(Response response, I input,
            TransactionHolder th) {
        if (response == null) {
            // Discarded
            return OptimisticLockingTransactionResultImpl.discarded(input);
        }
        return new OptimisticLockingTransactionResultImpl<>(th.discarded(), input, th.map(response));
    }

    @Override
    public Uni<Response> execute(String command, String... args) {
        nonNull(command, "command");
        return execute(Command.create(command), args);
    }

    @Override
    public Uni<Response> execute(Command command, String... args) {
        nonNull(command, "command");
        Request request = Request.cmd(command);
        for (String arg : args) {
            request.arg(arg);
        }
        return execute(request);
    }

    @Override
    public Uni<Response> execute(io.vertx.redis.client.Command command, String... args) {
        nonNull(command, "command");
        Request request = Request.newInstance(io.vertx.redis.client.Request.cmd(command));
        for (String arg : args) {
            request.arg(arg);
        }
        return execute(request);
    }

    @Override
    public Uni<Void> withConnection(Function<ReactiveRedisDataSource, Uni<Void>> function) {
        if (connection != null) {
            // We are already on a connection, keep this one
            return function.apply(this);
        }
        return redis.connect()
                .onItem().transformToUni(connection -> {
                    ReactiveRedisDataSourceImpl singleConnectionDS = new ReactiveRedisDataSourceImpl(vertx, redis, connection);
                    return function.apply(singleConnectionDS)
                            .onTermination().call(connection::close);
                });
    }

    @Override
    public Uni<Void> select(long index) {
        positiveOrZero(index, "index");
        return execute(Request.cmd(Command.SELECT).arg(index)).replaceWithVoid();
    }

    @Override
    public Uni<Void> flushall() {
        return execute(Request.cmd(Command.FLUSHALL)).replaceWithVoid();
    }

    @Override
    public <K, F, V> ReactiveHashCommands<K, F, V> hash(Class<K> redisKeyType, Class<F> fieldType, Class<V> valueType) {
        return new ReactiveHashCommandsImpl<>(this, redisKeyType, fieldType, valueType);
    }

    @Override
    public <K, F, V> ReactiveHashCommands<K, F, V> hash(TypeReference<K> redisKeyType, TypeReference<F> fieldType,
            TypeReference<V> valueType) {
        return new ReactiveHashCommandsImpl<>(this, redisKeyType.getType(), fieldType.getType(), valueType.getType());
    }

    @Override
    public <K, V> ReactiveGeoCommands<K, V> geo(Class<K> redisKeyType, Class<V> memberType) {
        return new ReactiveGeoCommandsImpl<>(this, redisKeyType, memberType);
    }

    @Override
    public <K, V> ReactiveGeoCommands<K, V> geo(TypeReference<K> redisKeyType, TypeReference<V> memberType) {
        return new ReactiveGeoCommandsImpl<>(this, redisKeyType.getType(), memberType.getType());
    }

    @Override
    public <K> ReactiveKeyCommands<K> key(Class<K> redisKeyType) {
        return new ReactiveKeyCommandsImpl<>(this, redisKeyType);
    }

    @Override
    public <K> ReactiveKeyCommands<K> key(TypeReference<K> redisKeyType) {
        return new ReactiveKeyCommandsImpl<>(this, redisKeyType.getType());
    }

    @Override
    public <K, V> ReactiveSortedSetCommands<K, V> sortedSet(Class<K> redisKeyType, Class<V> valueType) {
        return new ReactiveSortedSetCommandsImpl<>(this, redisKeyType, valueType);
    }

    @Override
    public <K, V> ReactiveSortedSetCommands<K, V> sortedSet(TypeReference<K> redisKeyType, TypeReference<V> valueType) {
        return new ReactiveSortedSetCommandsImpl<>(this, redisKeyType.getType(), valueType.getType());
    }

    @Override
    public <K, V> ReactiveStringCommands<K, V> string(Class<K> redisKeyType, Class<V> valueType) {
        return new ReactiveStringCommandsImpl<>(this, redisKeyType, valueType);
    }

    @Override
    public <K, V> ReactiveValueCommands<K, V> value(Class<K> redisKeyType, Class<V> valueType) {
        return new ReactiveStringCommandsImpl<>(this, redisKeyType, valueType);
    }

    @Override
    public <K, V> ReactiveValueCommands<K, V> value(TypeReference<K> redisKeyType, TypeReference<V> valueType) {
        return new ReactiveStringCommandsImpl<>(this, redisKeyType.getType(), valueType.getType());
    }

    @Override
    public <K, V> ReactiveSetCommands<K, V> set(Class<K> redisKeyType, Class<V> memberType) {
        return new ReactiveSetCommandsImpl<>(this, redisKeyType, memberType);
    }

    @Override
    public <K, V> ReactiveSetCommands<K, V> set(TypeReference<K> redisKeyType, TypeReference<V> memberType) {
        return new ReactiveSetCommandsImpl<>(this, redisKeyType.getType(), memberType.getType());
    }

    @Override
    public <K, V> ReactiveListCommands<K, V> list(Class<K> redisKeyType, Class<V> memberType) {
        return new ReactiveListCommandsImpl<>(this, redisKeyType, memberType);
    }

    @Override
    public <K, V> ReactiveListCommands<K, V> list(TypeReference<K> redisKeyType, TypeReference<V> memberType) {
        return new ReactiveListCommandsImpl<>(this, redisKeyType.getType(), memberType.getType());
    }

    @Override
    public <K, V> ReactiveHyperLogLogCommands<K, V> hyperloglog(Class<K> redisKeyType, Class<V> memberType) {
        return new ReactiveHyperLogLogCommandsImpl<>(this, redisKeyType, memberType);
    }

    @Override
    public <K, V> ReactiveHyperLogLogCommands<K, V> hyperloglog(TypeReference<K> redisKeyType, TypeReference<V> memberType) {
        return new ReactiveHyperLogLogCommandsImpl<>(this, redisKeyType.getType(), memberType.getType());
    }

    @Override
    public <K> ReactiveBitMapCommands<K> bitmap(Class<K> redisKeyType) {
        return new ReactiveBitMapCommandsImpl<>(this, redisKeyType);
    }

    @Override
    public <K> ReactiveBitMapCommands<K> bitmap(TypeReference<K> redisKeyType) {
        return new ReactiveBitMapCommandsImpl<>(this, redisKeyType.getType());
    }

    @Override
    public <K, F, V> ReactiveStreamCommands<K, F, V> stream(Class<K> redisKeyType, Class<F> fieldType, Class<V> valueType) {
        return new ReactiveStreamCommandsImpl<>(this, redisKeyType, fieldType, valueType);
    }

    @Override
    public <K, F, V> ReactiveStreamCommands<K, F, V> stream(TypeReference<K> redisKeyType, TypeReference<F> fieldType,
            TypeReference<V> valueType) {
        return new ReactiveStreamCommandsImpl<>(this, redisKeyType.getType(), fieldType.getType(), valueType.getType());
    }

    @Override
    public <K> ReactiveJsonCommands<K> json(Class<K> redisKeyType) {
        return new ReactiveJsonCommandsImpl<>(this, redisKeyType);
    }

    @Override
    public <K> ReactiveJsonCommands<K> json(TypeReference<K> redisKeyType) {
        return new ReactiveJsonCommandsImpl<>(this, redisKeyType.getType());
    }

    @Override
    public <K, V> ReactiveBloomCommands<K, V> bloom(Class<K> redisKeyType, Class<V> valueType) {
        return new ReactiveBloomCommandsImpl<>(this, redisKeyType, valueType);
    }

    @Override
    public <K, V> ReactiveBloomCommands<K, V> bloom(TypeReference<K> redisKeyType, TypeReference<V> valueType) {
        return new ReactiveBloomCommandsImpl<>(this, redisKeyType.getType(), valueType.getType());
    }

    @Override
    public <K, V> ReactiveCuckooCommands<K, V> cuckoo(Class<K> redisKeyType, Class<V> valueType) {
        return new ReactiveCuckooCommandsImpl<>(this, redisKeyType, valueType);
    }

    @Override
    public <K, V> ReactiveCuckooCommands<K, V> cuckoo(TypeReference<K> redisKeyType, TypeReference<V> valueType) {
        return new ReactiveCuckooCommandsImpl<>(this, redisKeyType.getType(), valueType.getType());
    }

    @Override
    public <K, V> ReactiveCountMinCommands<K, V> countmin(Class<K> redisKeyType, Class<V> valueType) {
        return new ReactiveCountMinCommandsImpl<>(this, redisKeyType, valueType);
    }

    @Override
    public <K, V> ReactiveCountMinCommands<K, V> countmin(TypeReference<K> redisKeyType, TypeReference<V> valueType) {
        return new ReactiveCountMinCommandsImpl<>(this, redisKeyType.getType(), valueType.getType());
    }

    @Override
    public <K, V> ReactiveTopKCommands<K, V> topk(Class<K> redisKeyType, Class<V> valueType) {
        return new ReactiveTopKCommandsImpl<>(this, redisKeyType, valueType);
    }

    @Override
    public <K, V> ReactiveTopKCommands<K, V> topk(TypeReference<K> redisKeyType, TypeReference<V> valueType) {
        return new ReactiveTopKCommandsImpl<>(this, redisKeyType.getType(), valueType.getType());
    }

    @Override
    public <K> ReactiveGraphCommands<K> graph(Class<K> redisKeyType) {
        return new ReactiveGraphCommandsImpl<>(this, redisKeyType);
    }

    @Override
    public <V> ReactivePubSubCommands<V> pubsub(Class<V> messageType) {
        return new ReactivePubSubCommandsImpl<>(this, messageType);
    }

    @Override
    public <V> ReactivePubSubCommands<V> pubsub(TypeReference<V> messageType) {
        return new ReactivePubSubCommandsImpl<>(this, messageType.getType());
    }

    @Override
    public <K> ReactiveSearchCommands<K> search(Class<K> redisKeyType) {
        return new ReactiveSearchCommandsImpl<>(this, redisKeyType);
    }

    @Override
    public <K> ReactiveAutoSuggestCommands<K> autosuggest(Class<K> redisKeyType) {
        return new ReactiveAutoSuggestCommandsImpl<>(this, redisKeyType);
    }

    @Override
    public <K> ReactiveAutoSuggestCommands<K> autosuggest(TypeReference<K> redisKeyType) {
        return new ReactiveAutoSuggestCommandsImpl<>(this, redisKeyType.getType());
    }

    @Override
    public <K> ReactiveTimeSeriesCommands<K> timeseries(Class<K> redisKeyType) {
        return new ReactiveTimeSeriesCommandsImpl<>(this, redisKeyType);
    }

    @Override
    public <K> ReactiveTimeSeriesCommands<K> timeseries(TypeReference<K> redisKeyType) {
        return new ReactiveTimeSeriesCommandsImpl<>(this, redisKeyType.getType());
    }

    @Override
    public Redis getRedis() {
        return redis;
    }

    public Vertx getVertx() {
        return vertx;
    }
}
