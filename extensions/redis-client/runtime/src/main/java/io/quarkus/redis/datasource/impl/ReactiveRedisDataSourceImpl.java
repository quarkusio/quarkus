package io.quarkus.redis.datasource.impl;

import static io.quarkus.redis.datasource.impl.Validation.notNullOrEmpty;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positiveOrZero;

import java.util.List;
import java.util.function.Function;

import io.quarkus.redis.datasource.api.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.api.bitmap.ReactiveBitMapCommands;
import io.quarkus.redis.datasource.api.geo.ReactiveGeoCommands;
import io.quarkus.redis.datasource.api.hash.ReactiveHashCommands;
import io.quarkus.redis.datasource.api.hyperloglog.ReactiveHyperLogLogCommands;
import io.quarkus.redis.datasource.api.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.api.list.ReactiveListCommands;
import io.quarkus.redis.datasource.api.pubsub.ReactivePubSubCommands;
import io.quarkus.redis.datasource.api.set.ReactiveSetCommands;
import io.quarkus.redis.datasource.api.sortedset.ReactiveSortedSetCommands;
import io.quarkus.redis.datasource.api.string.ReactiveStringCommands;
import io.quarkus.redis.datasource.api.transactions.ReactiveTransactionalRedisDataSource;
import io.quarkus.redis.datasource.api.transactions.TransactionResult;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.mutiny.redis.client.RedisConnection;
import io.vertx.mutiny.redis.client.Request;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveRedisDataSourceImpl implements ReactiveRedisDataSource, RedisCommandExecutor {

    final Redis redis;
    final RedisConnection connection;

    public ReactiveRedisDataSourceImpl(Redis redis, RedisAPI api) {
        nonNull(redis, "redis");
        nonNull(api, "api");
        this.redis = redis;
        this.connection = null;
    }

    public ReactiveRedisDataSourceImpl(Redis redis, RedisConnection connection) {
        nonNull(redis, "redis");
        nonNull(connection, "connection");
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
                    ReactiveRedisDataSourceImpl singleConnectionDS = new ReactiveRedisDataSourceImpl(redis, connection);
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
                    ReactiveRedisDataSourceImpl singleConnectionDS = new ReactiveRedisDataSourceImpl(redis, connection);
                    List<String> watched = List.of(keys);
                    TransactionHolder th = new TransactionHolder();
                    Request request = Request.cmd(Command.WATCH);
                    for (String s : watched) {
                        request.arg(s);
                    }
                    return connection.send(request)
                            .chain(() -> connection.send(Request.cmd(Command.MULTI))
                                    .chain(x -> function
                                            .apply(new ReactiveTransactionalRedisDataSourceImpl(singleConnectionDS, th)))
                                    .chain(ignored -> {
                                        if (!th.discarded()) {
                                            return connection.send(Request.cmd(Command.EXEC));
                                        } else {
                                            return Uni.createFrom().nullItem();
                                        }
                                    })
                                    .onTermination().call(connection::close)
                                    .map(r -> toTransactionResult(r, th)));
                });
    }

    public static TransactionResult toTransactionResult(Response response, TransactionHolder th) {
        if (response == null) {
            // Discarded
            return TransactionResultImpl.DISCARDED;
        }
        return new TransactionResultImpl(th.discarded(), th.map(response));
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
                    ReactiveRedisDataSourceImpl singleConnectionDS = new ReactiveRedisDataSourceImpl(redis, connection);
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
    public <K, V> ReactiveGeoCommands<K, V> geo(Class<K> redisKeyType, Class<V> memberType) {
        return new ReactiveGeoCommandsImpl<>(this, redisKeyType, memberType);
    }

    @Override
    public <K> ReactiveKeyCommands<K> key(Class<K> redisKeyType) {
        return new ReactiveKeyCommandsImpl<>(this, redisKeyType);
    }

    @Override
    public <K, V> ReactiveSortedSetCommands<K, V> sortedSet(Class<K> redisKeyType, Class<V> valueType) {
        return new ReactiveSortedSetCommandsImpl<>(this, redisKeyType, valueType);
    }

    @Override
    public <K, V> ReactiveStringCommands<K, V> string(Class<K> redisKeyType, Class<V> valueType) {
        return new ReactiveStringCommandsImpl<>(this, redisKeyType, valueType);
    }

    @Override
    public <K, V> ReactiveSetCommands<K, V> set(Class<K> redisKeyType, Class<V> memberType) {
        return new ReactiveSetCommandsImpl<>(this, redisKeyType, memberType);
    }

    @Override
    public <K, V> ReactiveListCommands<K, V> list(Class<K> redisKeyType, Class<V> memberType) {
        return new ReactiveListCommandsImpl<>(this, redisKeyType, memberType);
    }

    @Override
    public <K, V> ReactiveHyperLogLogCommands<K, V> hyperloglog(Class<K> redisKeyType, Class<V> memberType) {
        return new ReactiveHyperLogLogCommandsImpl<>(this, redisKeyType, memberType);
    }

    @Override
    public <K> ReactiveBitMapCommands<K> bitmap(Class<K> redisKeyType) {
        return new ReactiveBitMapCommandsImpl<>(this, redisKeyType);
    }

    @Override
    public <V> ReactivePubSubCommands<V> pubsub(Class<V> messageType) {
        return new ReactivePubSubCommandsImpl<>(this, messageType);
    }

    @Override
    public Redis getRedis() {
        return redis;
    }
}
