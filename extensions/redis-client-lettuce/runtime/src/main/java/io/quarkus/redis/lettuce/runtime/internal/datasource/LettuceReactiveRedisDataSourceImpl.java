package io.quarkus.redis.lettuce.runtime.internal.datasource;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positiveOrZero;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
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
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnection;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnectionPool;
import io.quarkus.redis.lettuce.runtime.internal.LettuceResult;
import io.quarkus.redis.lettuce.runtime.internal.autosuggest.LettuceReactiveAutoSuggestCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.bitmap.LettuceReactiveBitMapCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.bloom.LettuceReactiveBloomCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.countmin.LettuceReactiveCountMinCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.cuckoo.LettuceReactiveCuckooCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.geo.LettuceReactiveGeoCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.hash.LettuceReactiveHashCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.hyperloglog.LettuceReactiveHyperLogLogCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.json.LettuceReactiveJsonCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.key.LettuceReactiveKeyCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.list.LettuceReactiveListCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.pubsub.LettuceReactivePubSubCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.search.LettuceReactiveSearchCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.set.LettuceReactiveSetCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.sortedset.LettuceReactiveSortedSetCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.stream.LettuceReactiveStreamCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.topk.LettuceReactiveTopKCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.value.LettuceReactiveValueCommandsImpl;
import io.quarkus.redis.runtime.datasource.OptimisticLockingTransactionResultImpl;
import io.quarkus.redis.runtime.datasource.TransactionResultImpl;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Request;
import io.vertx.redis.client.Response;
import io.vertx.redis.client.impl.CommandMap;
import io.vertx.redis.client.impl.RequestImpl;
import tools.jackson.core.type.TypeReference;

/**
 * Lettuce-backed implementation of {@link ReactiveRedisDataSource}.
 * <p>
 * Wires the implemented command groups to their Lettuce impls (see the {@code lettuce/<group>}
 * packages, e.g. {@link LettuceReactiveValueCommandsImpl}) and implements {@code execute(...)},
 * {@code flushall()} and {@code select(...)} on top of the Lettuce async API. The connection is a
 * {@link LettuceConnection}, to a standalone server or to a cluster: on a cluster every command is
 * routed to the node owning its key, {@code flushall()} runs on every upstream node, {@code select(...)}
 * is rejected by the server and {@code withTransaction(...)} fails at once (see
 * {@link LettuceConnection#TRANSACTIONS_NOT_SUPPORTED_ON_CLUSTER}).
 * {@code withConnection(...)} runs the user block on a connection borrowed from the {@code pool};
 * {@code withTransaction(...)} runs it on a pinned, pooled connection under {@code MULTI}/{@code EXEC},
 * including the {@code WATCH}-based optimistic-locking variants. A {@code SELECT} issued on a pinned
 * data source is reported to the pool so the connection is put back on its default database before
 * the next borrower gets it. Blocking commands issued directly on
 * this (non-pinned) data source likewise borrow a pooled connection instead of running on the shared
 * one, so they cannot head-of-line-block ordinary traffic; see {@link LettuceReactiveListCommandsImpl}
 * and {@link io.quarkus.redis.lettuce.runtime.internal.sortedset.LettuceReactiveSortedSetCommandsImpl}.
 * Pub/Sub subscriptions open their own connection through {@code pubSubConnector}: a connection in
 * subscribed mode cannot run ordinary commands, so it is neither the shared connection nor pooled.
 * {@code getRedis()} throws {@link UnsupportedOperationException}: it returns a Vert.x-specific type
 * that has no Lettuce equivalent.
 */
public class LettuceReactiveRedisDataSourceImpl implements ReactiveRedisDataSource {

    private final Vertx vertx;
    private final LettuceConnection connection;
    private final LettuceConnectionPool pool;
    private final Supplier<CompletionStage<StatefulRedisPubSubConnection<byte[], byte[]>>> pubSubConnector;
    private final boolean pinned;

    public LettuceReactiveRedisDataSourceImpl(Vertx vertx, LettuceConnection connection, LettuceConnectionPool pool,
            Supplier<CompletionStage<StatefulRedisPubSubConnection<byte[], byte[]>>> pubSubConnector) {
        this(vertx, connection, pool, pubSubConnector, false);
    }

    private LettuceReactiveRedisDataSourceImpl(Vertx vertx, LettuceConnection connection, LettuceConnectionPool pool,
            Supplier<CompletionStage<StatefulRedisPubSubConnection<byte[], byte[]>>> pubSubConnector, boolean pinned) {
        this.vertx = nonNull(vertx, "vertx");
        this.connection = nonNull(connection, "connection");
        this.pool = pool;
        this.pubSubConnector = nonNull(pubSubConnector, "pubSubConnector");
        this.pinned = pinned;
    }

    /**
     * Creates a data source pinned to {@code conn}, which was borrowed from this data source's pool. The
     * pool is only used to report a {@code SELECT} on the connection; scoped operations reuse the
     * pinned connection and blocking commands run on it directly. Pub/Sub subscriptions still open
     * their own connection through the shared connector.
     */
    LettuceReactiveRedisDataSourceImpl pinnedTo(LettuceConnection conn) {
        return new LettuceReactiveRedisDataSourceImpl(vertx, conn, pool, pubSubConnector, true);
    }

    /** The pool blocking commands borrow from: none on a pinned data source. */
    private LettuceConnectionPool blockingPool() {
        return pinned ? null : pool;
    }

    /**
     * Tells the pool that the pinned connection may have left its default database, so it is
     * reset before being reused. Called before the {@code SELECT} is sent: a {@code SELECT} that
     * times out may still have been applied by Redis.
     */
    private void markDatabaseDirty() {
        if (pinned && pool != null) {
            pool.markDirty(connection);
        }
    }

    public Vertx getVertx() {
        return vertx;
    }

    public LettuceConnection getConnection() {
        return connection;
    }

    public LettuceConnectionPool getPool() {
        return pool;
    }

    @Override
    public Uni<Response> execute(String command, String... args) {
        nonNull(command, "command");
        return dispatch(resolve(command), rawArgs(CommandMap.getKnownCommand(command), args));
    }

    @Override
    public Uni<Response> execute(Command command, String... args) {
        nonNull(command, "command");
        return dispatch(resolve(command.toString()), rawArgs(command, args));
    }

    private Uni<Response> dispatch(ProtocolKeyword type, CommandArgs<byte[], byte[]> commandArgs) {
        if (type == CommandType.SELECT) {
            markDatabaseDirty();
        }
        LettuceVertxResponseOutput<byte[], byte[]> output = new LettuceVertxResponseOutput<>(ByteArrayCodec.INSTANCE);
        return LettuceResult.toUni(() -> connection.async().dispatch(type, output, commandArgs))
                .map(ignored -> output.toVertxResponse());
    }

    /**
     * The arguments of a raw command, the keys among them added as keys: a cluster connection routes a command to
     * the node owning its first key, and would send a command without one to a default node and follow the
     * {@code MOVED} redirect. Which arguments are keys comes from the Vert.x client's command table, the same
     * knowledge its cluster connection routes on; a command unknown to it ({@code null}, or one created with
     * {@code Command.create(...)}) has no key there either. {@code null} arguments are skipped.
     *
     * @param command the Vert.x command, {@code null} when unknown
     */
    static CommandArgs<byte[], byte[]> rawArgs(Command command, String... args) {
        CommandArgs<byte[], byte[]> commandArgs = new CommandArgs<>(ByteArrayCodec.INSTANCE);
        if (args == null) {
            return commandArgs;
        }
        List<byte[]> keys = command == null ? List.of() : keysOf(command, args);
        for (String arg : args) {
            if (arg == null) {
                continue;
            }
            byte[] bytes = arg.getBytes(StandardCharsets.UTF_8);
            if (isKey(bytes, keys)) {
                commandArgs.addKey(bytes);
            } else {
                commandArgs.add(bytes);
            }
        }
        return commandArgs;
    }

    private static List<byte[]> keysOf(Command command, String[] args) {
        Request request = Request.cmd(command);
        for (String arg : args) {
            if (arg != null) {
                request.arg(arg);
            }
        }
        try {
            return ((RequestImpl) request).keys();
        } catch (RuntimeException malformed) {
            // arguments the key locators cannot make sense of: let the server answer
            return List.of();
        }
    }

    private static boolean isKey(byte[] bytes, List<byte[]> keys) {
        for (byte[] key : keys) {
            if (Arrays.equals(key, bytes)) {
                return true;
            }
        }
        return false;
    }

    static ProtocolKeyword resolve(String name) {
        try {
            return CommandType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return new StringKeyword(name.toUpperCase());
        }
    }

    @Override
    public Uni<Void> flushall() {
        return LettuceResult.toUni(() -> connection.async().flushall()).replaceWithVoid();
    }

    @Override
    public Uni<Void> select(long index) {
        positiveOrZero(index, "index");
        markDatabaseDirty();
        return LettuceResult.toUni(() -> connection.select((int) index)).replaceWithVoid();
    }

    @Override
    public Redis getRedis() {
        throw new UnsupportedOperationException(
                "getRedis() is not supported on the Lettuce backend: it returns a Vert.x-specific type "
                        + "(io.vertx.mutiny.redis.client.Redis) that has no Lettuce equivalent. "
                        + "Either use execute(command, args...) for raw Redis commands, "
                        + "or remove the quarkus-redis-client-lettuce extension to use the Vert.x backend.");
    }

    LettuceConnection acquireConnection(Duration timeout) {
        return pool.acquireBlocking(timeout);
    }

    Uni<Void> releaseConnection(LettuceConnection conn) {
        return pool.release(conn);
    }

    @Override
    public Uni<Void> withConnection(Function<ReactiveRedisDataSource, Uni<Void>> function) {
        if (pinned) {
            return function.apply(this);
        }
        return pool.withScoped(conn -> function.apply(pinnedTo(conn)));
    }

    @Override
    public Uni<TransactionResult> withTransaction(Function<ReactiveTransactionalRedisDataSource, Uni<Void>> tx) {
        nonNull(tx, "tx");
        return withTxConnection(conn -> runTx(conn, tx, null));
    }

    @Override
    public Uni<TransactionResult> withTransaction(Function<ReactiveTransactionalRedisDataSource, Uni<Void>> tx,
            String... watchedKeys) {
        nonNull(tx, "tx");
        notNullOrEmpty(watchedKeys, "watchedKeys");
        doesNotContainNull(watchedKeys, "watchedKeys");
        return withTxConnection(conn -> runTx(conn, tx, watchedKeys));
    }

    @Override
    public <I> Uni<OptimisticLockingTransactionResult<I>> withTransaction(
            Function<ReactiveRedisDataSource, Uni<I>> preTx,
            BiFunction<I, ReactiveTransactionalRedisDataSource, Uni<Void>> tx,
            String... watchedKeys) {
        nonNull(preTx, "preTx");
        nonNull(tx, "tx");
        notNullOrEmpty(watchedKeys, "watchedKeys");
        doesNotContainNull(watchedKeys, "watchedKeys");
        return withTxConnection(conn -> runOptimisticTx(conn, preTx, tx, watchedKeys));
    }

    /**
     * Runs {@code body} on a transaction-scoped connection. When this data source is already
     * pinned (nested inside {@code withConnection}), the pinned connection is reused and left
     * open for the outer scope to release. Otherwise, a connection is borrowed from the pool and
     * released on every termination path. On a cluster there is no transaction to run: the
     * returned {@code Uni} fails at once, before any connection is borrowed, with the message
     * of {@link LettuceConnection#TRANSACTIONS_NOT_SUPPORTED_ON_CLUSTER}.
     */
    private <T> Uni<T> withTxConnection(Function<LettuceConnection, Uni<T>> body) {
        if (connection.isCluster()) {
            return Uni.createFrom()
                    .failure(new UnsupportedOperationException(LettuceConnection.TRANSACTIONS_NOT_SUPPORTED_ON_CLUSTER));
        }
        if (pinned) {
            return Uni.createFrom().deferred(() -> body.apply(connection));
        }
        return pool.withScoped(body);
    }

    private Uni<TransactionResult> runTx(LettuceConnection conn,
            Function<ReactiveTransactionalRedisDataSource, Uni<Void>> tx, String[] watchedKeys) {
        LettuceReactiveRedisDataSourceImpl pinnedDs = pinnedTo(conn);
        LettuceTransactionHolder holder = new LettuceTransactionHolder();
        LettuceReactiveTransactionalRedisDataSourceImpl txDs = new LettuceReactiveTransactionalRedisDataSourceImpl(
                pinnedDs, holder);

        Uni<Void> watch = watchedKeys == null ? Uni.createFrom().voidItem() : watch(conn, watchedKeys);
        return watch
                .chain(() -> LettuceResult.toUni(conn::multi).replaceWithVoid())
                .chain(() -> Uni.createFrom().deferred(() -> tx.apply(txDs)))
                .onItemOrFailure().transformToUni((x, failure) -> {
                    if (failure != null) {
                        return abort(conn, holder, failure);
                    }
                    if (holder.discarded()) {
                        return Uni.createFrom().item(TransactionResultImpl.DISCARDED);
                    }
                    return LettuceResult.toUni(conn::exec)
                            .chain(execResult -> execResult == null || execResult.wasDiscarded()
                                    ? Uni.createFrom().item(TransactionResultImpl.DISCARDED)
                                    : Uni.createFrom().item(holder.toResult(execResult)));
                });
    }

    private <I> Uni<OptimisticLockingTransactionResult<I>> runOptimisticTx(LettuceConnection conn,
            Function<ReactiveRedisDataSource, Uni<I>> preTx,
            BiFunction<I, ReactiveTransactionalRedisDataSource, Uni<Void>> tx, String[] watchedKeys) {
        LettuceReactiveRedisDataSourceImpl pinnedDs = pinnedTo(conn);
        LettuceTransactionHolder holder = new LettuceTransactionHolder();
        LettuceReactiveTransactionalRedisDataSourceImpl txDs = new LettuceReactiveTransactionalRedisDataSourceImpl(
                pinnedDs, holder);

        return watch(conn, watchedKeys)
                .chain(() -> Uni.createFrom().deferred(() -> preTx.apply(pinnedDs)))
                .onFailure().recoverWithUni(failure -> LettuceResult.toUni(conn::unwatch)
                        .onItemOrFailure().transformToUni((r, f) -> {
                            if (f != null) {
                                failure.addSuppressed(f);
                            }
                            return Uni.createFrom().failure(failure);
                        }))
                .chain(input -> LettuceResult.toUni(conn::multi).replaceWithVoid()
                        .chain(() -> Uni.createFrom().deferred(() -> tx.apply(input, txDs)))
                        .onItemOrFailure().transformToUni((x, failure) -> {
                            if (failure != null) {
                                return abort(conn, holder, failure);
                            }
                            if (holder.discarded()) {
                                return Uni.createFrom().item(OptimisticLockingTransactionResultImpl.discarded(input));
                            }
                            return LettuceResult.toUni(conn::exec)
                                    .chain(execResult -> execResult == null || execResult.wasDiscarded()
                                            ? Uni.createFrom()
                                                    .item(OptimisticLockingTransactionResultImpl.discarded(input))
                                            : Uni.createFrom().item(holder.toOptimisticLockingResult(input, execResult)));
                        }));
    }

    private Uni<Void> watch(LettuceConnection conn, String[] keys) {
        byte[][] encodedKeys = encodeKeys(keys);
        return LettuceResult.toUni(() -> conn.watch(encodedKeys)).replaceWithVoid();
    }

    static byte[][] encodeKeys(String[] keys) {
        byte[][] encodedKeys = new byte[keys.length][];
        for (int i = 0; i < keys.length; i++) {
            encodedKeys[i] = keys[i].getBytes(StandardCharsets.UTF_8);
        }
        return encodedKeys;
    }

    /**
     * Aborts an in-flight transaction after the user block failed: issues {@code DISCARD}
     * (unless the user already discarded) and re-propagates the original failure, attaching any
     * {@code DISCARD} failure as suppressed. Mirrors the Vert.x backend's abort path.
     */
    private static <T> Uni<T> abort(LettuceConnection conn, LettuceTransactionHolder holder,
            Throwable failure) {
        if (holder.discarded()) {
            return Uni.createFrom().failure(failure);
        }
        return LettuceResult.toUni(conn::discard)
                .onItemOrFailure().transformToUni((r, f) -> {
                    if (f != null) {
                        failure.addSuppressed(f);
                    }
                    return Uni.createFrom().failure(failure);
                });
    }

    @Override
    public <K, V> ReactiveValueCommands<K, V> value(Class<K> redisKeyType, Class<V> valueType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(valueType, "valueType");
        return new LettuceReactiveValueCommandsImpl<>(this, connection, redisKeyType, valueType);
    }

    @Override
    public <K, V> ReactiveValueCommands<K, V> value(TypeReference<K> redisKeyType, TypeReference<V> valueType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(valueType, "valueType");
        return new LettuceReactiveValueCommandsImpl<>(this, connection, redisKeyType.getType(), valueType.getType());
    }

    @Override
    public <K, V> ReactiveStringCommands<K, V> string(Class<K> redisKeyType, Class<V> valueType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(valueType, "valueType");
        return new LettuceReactiveValueCommandsImpl<>(this, connection, redisKeyType, valueType);
    }

    @Override
    public <K, F, V> ReactiveHashCommands<K, F, V> hash(Class<K> redisKeyType, Class<F> fieldType, Class<V> valueType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(fieldType, "fieldType");
        nonNull(valueType, "valueType");
        return new LettuceReactiveHashCommandsImpl<>(this, connection, redisKeyType, fieldType, valueType);
    }

    @Override
    public <K, F, V> ReactiveHashCommands<K, F, V> hash(TypeReference<K> redisKeyType, TypeReference<F> fieldType,
            TypeReference<V> valueType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(fieldType, "fieldType");
        nonNull(valueType, "valueType");
        return new LettuceReactiveHashCommandsImpl<>(this, connection, redisKeyType.getType(), fieldType.getType(),
                valueType.getType());
    }

    @Override
    public <K, V> ReactiveGeoCommands<K, V> geo(Class<K> redisKeyType, Class<V> memberType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(memberType, "memberType");
        return new LettuceReactiveGeoCommandsImpl<>(this, connection, redisKeyType, memberType);
    }

    @Override
    public <K, V> ReactiveGeoCommands<K, V> geo(TypeReference<K> redisKeyType, TypeReference<V> memberType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(memberType, "memberType");
        return new LettuceReactiveGeoCommandsImpl<>(this, connection, redisKeyType.getType(), memberType.getType());
    }

    @Override
    public <K> ReactiveKeyCommands<K> key(Class<K> redisKeyType) {
        nonNull(redisKeyType, "redisKeyType");
        return new LettuceReactiveKeyCommandsImpl<>(this, connection, redisKeyType);
    }

    @Override
    public <K> ReactiveKeyCommands<K> key(TypeReference<K> redisKeyType) {
        nonNull(redisKeyType, "redisKeyType");
        return new LettuceReactiveKeyCommandsImpl<>(this, connection, redisKeyType.getType());
    }

    @Override
    public <K, V> ReactiveSortedSetCommands<K, V> sortedSet(Class<K> redisKeyType, Class<V> valueType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(valueType, "valueType");
        return new LettuceReactiveSortedSetCommandsImpl<>(this, connection, blockingPool(), redisKeyType, valueType);
    }

    @Override
    public <K, V> ReactiveSortedSetCommands<K, V> sortedSet(TypeReference<K> redisKeyType, TypeReference<V> valueType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(valueType, "valueType");
        return new LettuceReactiveSortedSetCommandsImpl<>(this, connection, blockingPool(), redisKeyType.getType(),
                valueType.getType());
    }

    @Override
    public <K, V> ReactiveSetCommands<K, V> set(Class<K> redisKeyType, Class<V> memberType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(memberType, "memberType");
        return new LettuceReactiveSetCommandsImpl<>(this, connection, redisKeyType, memberType);
    }

    @Override
    public <K, V> ReactiveSetCommands<K, V> set(TypeReference<K> redisKeyType, TypeReference<V> memberType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(memberType, "memberType");
        return new LettuceReactiveSetCommandsImpl<>(this, connection, redisKeyType.getType(), memberType.getType());
    }

    @Override
    public <K, V> ReactiveListCommands<K, V> list(Class<K> redisKeyType, Class<V> memberType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(memberType, "memberType");
        return new LettuceReactiveListCommandsImpl<>(this, connection, blockingPool(), redisKeyType, memberType);
    }

    @Override
    public <K, V> ReactiveListCommands<K, V> list(TypeReference<K> redisKeyType, TypeReference<V> memberType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(memberType, "memberType");
        return new LettuceReactiveListCommandsImpl<>(this, connection, blockingPool(), redisKeyType.getType(),
                memberType.getType());
    }

    @Override
    public <K, V> ReactiveHyperLogLogCommands<K, V> hyperloglog(Class<K> redisKeyType, Class<V> memberType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(memberType, "memberType");
        return new LettuceReactiveHyperLogLogCommandsImpl<>(this, connection, redisKeyType, memberType);
    }

    @Override
    public <K, V> ReactiveHyperLogLogCommands<K, V> hyperloglog(TypeReference<K> redisKeyType, TypeReference<V> memberType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(memberType, "memberType");
        return new LettuceReactiveHyperLogLogCommandsImpl<>(this, connection, redisKeyType.getType(), memberType.getType());
    }

    @Override
    public <K> ReactiveBitMapCommands<K> bitmap(Class<K> redisKeyType) {
        nonNull(redisKeyType, "redisKeyType");
        return new LettuceReactiveBitMapCommandsImpl<>(this, connection, redisKeyType);
    }

    @Override
    public <K> ReactiveBitMapCommands<K> bitmap(TypeReference<K> redisKeyType) {
        nonNull(redisKeyType, "redisKeyType");
        return new LettuceReactiveBitMapCommandsImpl<>(this, connection, redisKeyType.getType());
    }

    @Override
    public <K, F, V> ReactiveStreamCommands<K, F, V> stream(Class<K> redisKeyType, Class<F> fieldType, Class<V> valueType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(fieldType, "fieldType");
        nonNull(valueType, "valueType");
        return new LettuceReactiveStreamCommandsImpl<>(this, connection, blockingPool(), redisKeyType, fieldType, valueType);
    }

    @Override
    public <K, F, V> ReactiveStreamCommands<K, F, V> stream(TypeReference<K> redisKeyType, TypeReference<F> fieldType,
            TypeReference<V> valueType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(fieldType, "fieldType");
        nonNull(valueType, "valueType");
        return new LettuceReactiveStreamCommandsImpl<>(this, connection, blockingPool(), redisKeyType.getType(),
                fieldType.getType(), valueType.getType());
    }

    @Override
    public <V> ReactivePubSubCommands<V> pubsub(Class<V> messageType) {
        nonNull(messageType, "messageType");
        return new LettuceReactivePubSubCommandsImpl<>(this, connection, pubSubConnector, messageType);
    }

    @Override
    public <V> ReactivePubSubCommands<V> pubsub(TypeReference<V> messageType) {
        nonNull(messageType, "messageType");
        return new LettuceReactivePubSubCommandsImpl<>(this, connection, pubSubConnector, messageType.getType());
    }

    @Override
    public <K> ReactiveJsonCommands<K> json(Class<K> redisKeyType) {
        nonNull(redisKeyType, "redisKeyType");
        return new LettuceReactiveJsonCommandsImpl<>(this, connection, redisKeyType);
    }

    @Override
    public <K> ReactiveJsonCommands<K> json(TypeReference<K> redisKeyType) {
        nonNull(redisKeyType, "redisKeyType");
        return new LettuceReactiveJsonCommandsImpl<>(this, connection, redisKeyType.getType());
    }

    @Override
    public <K, V> ReactiveBloomCommands<K, V> bloom(Class<K> redisKeyType, Class<V> valueType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(valueType, "valueType");
        return new LettuceReactiveBloomCommandsImpl<>(this, connection, redisKeyType, valueType);
    }

    @Override
    public <K, V> ReactiveBloomCommands<K, V> bloom(TypeReference<K> redisKeyType, TypeReference<V> valueType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(valueType, "valueType");
        return new LettuceReactiveBloomCommandsImpl<>(this, connection, redisKeyType.getType(), valueType.getType());
    }

    @Override
    public <K, V> ReactiveCuckooCommands<K, V> cuckoo(Class<K> redisKeyType, Class<V> valueType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(valueType, "valueType");
        return new LettuceReactiveCuckooCommandsImpl<>(this, connection, redisKeyType, valueType);
    }

    @Override
    public <K, V> ReactiveCuckooCommands<K, V> cuckoo(TypeReference<K> redisKeyType, TypeReference<V> valueType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(valueType, "valueType");
        return new LettuceReactiveCuckooCommandsImpl<>(this, connection, redisKeyType.getType(), valueType.getType());
    }

    @Override
    public <K, V> ReactiveCountMinCommands<K, V> countmin(Class<K> redisKeyType, Class<V> valueType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(valueType, "valueType");
        return new LettuceReactiveCountMinCommandsImpl<>(this, connection, redisKeyType, valueType);
    }

    @Override
    public <K, V> ReactiveCountMinCommands<K, V> countmin(TypeReference<K> redisKeyType, TypeReference<V> valueType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(valueType, "valueType");
        return new LettuceReactiveCountMinCommandsImpl<>(this, connection, redisKeyType.getType(), valueType.getType());
    }

    @Override
    public <K, V> ReactiveTopKCommands<K, V> topk(Class<K> redisKeyType, Class<V> valueType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(valueType, "valueType");
        return new LettuceReactiveTopKCommandsImpl<>(this, connection, redisKeyType, valueType);
    }

    @Override
    public <K, V> ReactiveTopKCommands<K, V> topk(TypeReference<K> redisKeyType, TypeReference<V> valueType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(valueType, "valueType");
        return new LettuceReactiveTopKCommandsImpl<>(this, connection, redisKeyType.getType(), valueType.getType());
    }

    @Override
    public <K> ReactiveGraphCommands<K> graph(Class<K> redisKeyType) {
        throw groupNotImplemented("graph");
    }

    @Override
    public <K> ReactiveSearchCommands<K> search(Class<K> redisKeyType) {
        nonNull(redisKeyType, "redisKeyType");
        return new LettuceReactiveSearchCommandsImpl<>(this, connection, redisKeyType);
    }

    @Override
    public <K> ReactiveAutoSuggestCommands<K> autosuggest(Class<K> redisKeyType) {
        nonNull(redisKeyType, "redisKeyType");
        return new LettuceReactiveAutoSuggestCommandsImpl<>(this, connection, redisKeyType);
    }

    @Override
    public <K> ReactiveAutoSuggestCommands<K> autosuggest(TypeReference<K> redisKeyType) {
        nonNull(redisKeyType, "redisKeyType");
        return new LettuceReactiveAutoSuggestCommandsImpl<>(this, connection, redisKeyType.getType());
    }

    @Override
    public <K> ReactiveTimeSeriesCommands<K> timeseries(Class<K> redisKeyType) {
        throw groupNotImplemented("timeseries");
    }

    @Override
    public <K> ReactiveTimeSeriesCommands<K> timeseries(TypeReference<K> redisKeyType) {
        throw groupNotImplemented("timeseries");
    }

    private static UnsupportedOperationException groupNotImplemented(String group) {
        return new UnsupportedOperationException(
                "The '" + group + "' command group is not yet implemented on the Lettuce backend. "
                        + "Remove the quarkus-redis-client-lettuce extension to use the Vert.x backend.");
    }

    /**
     * Fallback {@link ProtocolKeyword} for raw command names not present in
     * {@link CommandType}. Allows {@code execute("DEBUG", "SLEEP", "0")} and similar
     * extension/module commands to be dispatched without requiring an enum entry.
     */
    private static final class StringKeyword implements ProtocolKeyword {

        private final String name;
        private final byte[] bytes;

        StringKeyword(String name) {
            this.name = name;
            this.bytes = name.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public String toString() {
            return name;
        }
    }

}
