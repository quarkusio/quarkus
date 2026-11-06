package io.quarkus.redis.lettuce.runtime.internal.datasource;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.autosuggest.AutoSuggestCommands;
import io.quarkus.redis.datasource.autosuggest.ReactiveAutoSuggestCommands;
import io.quarkus.redis.datasource.bitmap.BitMapCommands;
import io.quarkus.redis.datasource.bitmap.ReactiveBitMapCommands;
import io.quarkus.redis.datasource.bloom.BloomCommands;
import io.quarkus.redis.datasource.bloom.ReactiveBloomCommands;
import io.quarkus.redis.datasource.countmin.CountMinCommands;
import io.quarkus.redis.datasource.countmin.ReactiveCountMinCommands;
import io.quarkus.redis.datasource.cuckoo.CuckooCommands;
import io.quarkus.redis.datasource.cuckoo.ReactiveCuckooCommands;
import io.quarkus.redis.datasource.geo.GeoCommands;
import io.quarkus.redis.datasource.geo.ReactiveGeoCommands;
import io.quarkus.redis.datasource.graph.GraphCommands;
import io.quarkus.redis.datasource.hash.HashCommands;
import io.quarkus.redis.datasource.hash.ReactiveHashCommands;
import io.quarkus.redis.datasource.hyperloglog.HyperLogLogCommands;
import io.quarkus.redis.datasource.hyperloglog.ReactiveHyperLogLogCommands;
import io.quarkus.redis.datasource.json.JsonCommands;
import io.quarkus.redis.datasource.json.ReactiveJsonCommands;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.list.ListCommands;
import io.quarkus.redis.datasource.list.ReactiveListCommands;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.redis.datasource.pubsub.ReactivePubSubCommands;
import io.quarkus.redis.datasource.search.ReactiveSearchCommands;
import io.quarkus.redis.datasource.search.SearchCommands;
import io.quarkus.redis.datasource.set.ReactiveSetCommands;
import io.quarkus.redis.datasource.set.SetCommands;
import io.quarkus.redis.datasource.sortedset.ReactiveSortedSetCommands;
import io.quarkus.redis.datasource.sortedset.SortedSetCommands;
import io.quarkus.redis.datasource.stream.ReactiveStreamCommands;
import io.quarkus.redis.datasource.stream.StreamCommands;
import io.quarkus.redis.datasource.string.StringCommands;
import io.quarkus.redis.datasource.timeseries.TimeSeriesCommands;
import io.quarkus.redis.datasource.topk.ReactiveTopKCommands;
import io.quarkus.redis.datasource.topk.TopKCommands;
import io.quarkus.redis.datasource.transactions.OptimisticLockingTransactionResult;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.datasource.transactions.TransactionalRedisDataSource;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnection;
import io.quarkus.redis.lettuce.runtime.internal.LettuceResult;
import io.quarkus.redis.runtime.datasource.BlockingAutoSuggestCommandsImpl;
import io.quarkus.redis.runtime.datasource.BlockingBitmapCommandsImpl;
import io.quarkus.redis.runtime.datasource.BlockingBloomCommandsImpl;
import io.quarkus.redis.runtime.datasource.BlockingCountMinCommandsImpl;
import io.quarkus.redis.runtime.datasource.BlockingCuckooCommandsImpl;
import io.quarkus.redis.runtime.datasource.BlockingGeoCommandsImpl;
import io.quarkus.redis.runtime.datasource.BlockingHashCommandsImpl;
import io.quarkus.redis.runtime.datasource.BlockingHyperLogLogCommandsImpl;
import io.quarkus.redis.runtime.datasource.BlockingJsonCommandsImpl;
import io.quarkus.redis.runtime.datasource.BlockingKeyCommandsImpl;
import io.quarkus.redis.runtime.datasource.BlockingListCommandsImpl;
import io.quarkus.redis.runtime.datasource.BlockingPubSubCommandsImpl;
import io.quarkus.redis.runtime.datasource.BlockingSearchCommandsImpl;
import io.quarkus.redis.runtime.datasource.BlockingSetCommandsImpl;
import io.quarkus.redis.runtime.datasource.BlockingSortedSetCommandsImpl;
import io.quarkus.redis.runtime.datasource.BlockingStreamCommandsImpl;
import io.quarkus.redis.runtime.datasource.BlockingStringCommandsImpl;
import io.quarkus.redis.runtime.datasource.BlockingTopKCommandsImpl;
import io.quarkus.redis.runtime.datasource.BlockingTransactionalRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.OptimisticLockingTransactionResultImpl;
import io.quarkus.redis.runtime.datasource.TransactionResultImpl;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Response;
import tools.jackson.core.type.TypeReference;

/**
 * Blocking wrapper around {@link LettuceReactiveRedisDataSourceImpl}.
 * <p>
 * Each method awaits the reactive result for the configured {@code timeout}. Must not be
 * invoked from an event loop thread. {@code withConnection(...)} runs the user block on a
 * dedicated connection borrowed from the underlying reactive data source.
 */
public class LettuceBlockingRedisDataSourceImpl implements RedisDataSource {

    private static final Logger LOGGER = Logger.getLogger(LettuceBlockingRedisDataSourceImpl.class);

    private final LettuceReactiveRedisDataSourceImpl reactive;
    private final Duration timeout;
    private final boolean pinned;

    public LettuceBlockingRedisDataSourceImpl(LettuceReactiveRedisDataSourceImpl reactive, Duration timeout) {
        this(reactive, timeout, false);
    }

    private LettuceBlockingRedisDataSourceImpl(LettuceReactiveRedisDataSourceImpl reactive, Duration timeout, boolean pinned) {
        this.reactive = nonNull(reactive, "reactive");
        this.timeout = nonNull(timeout, "timeout");
        this.pinned = pinned;
    }

    static LettuceBlockingRedisDataSourceImpl pinnedTo(LettuceReactiveRedisDataSourceImpl pinnedReactive, Duration timeout) {
        return new LettuceBlockingRedisDataSourceImpl(pinnedReactive, timeout, true);
    }

    @Override
    public ReactiveRedisDataSource getReactive() {
        return reactive;
    }

    @Override
    public Response execute(String command, String... args) {
        return reactive.execute(command, args).await().atMost(timeout);
    }

    @Override
    public Response execute(Command command, String... args) {
        return reactive.execute(command, args).await().atMost(timeout);
    }

    @Override
    public void flushall() {
        reactive.flushall().await().atMost(timeout);
    }

    @Override
    public void select(long index) {
        reactive.select(index).await().atMost(timeout);
    }

    @Override
    public void withConnection(Consumer<RedisDataSource> consumer) {
        if (pinned) {
            consumer.accept(this);
            return;
        }
        // Acquire the connection and run the user block on the calling (worker) thread. Running it
        // inside the reactive withConnection pipeline would execute it on the event loop thread
        // that completed the connection, where the block's blocking calls would deadlock.
        LettuceConnection conn = reactive.acquireConnection(timeout);
        releasing(conn, () -> {
            LettuceReactiveRedisDataSourceImpl pinnedReactive = reactive.pinnedTo(conn);
            consumer.accept(pinnedTo(pinnedReactive, timeout));
            return null;
        });
    }

    @Override
    public TransactionResult withTransaction(Consumer<TransactionalRedisDataSource> tx) {
        nonNull(tx, "tx");
        LettuceConnection conn = acquire();
        return releasing(conn, () -> {
            LettuceTransactionHolder holder = new LettuceTransactionHolder();
            BlockingTransactionalRedisDataSourceImpl source = transactionalSource(conn, holder);
            LettuceResult.toBlocking(conn.multi(), timeout);
            runTxBlock(conn, source, () -> tx.accept(source));
            return assembleResult(conn, holder, source.discarded());
        });
    }

    @Override
    public TransactionResult withTransaction(Consumer<TransactionalRedisDataSource> tx, String... watchedKeys) {
        nonNull(tx, "tx");
        notNullOrEmpty(watchedKeys, "watchedKeys");
        doesNotContainNull(watchedKeys, "watchedKeys");
        LettuceConnection conn = acquire();
        return releasing(conn, () -> {
            LettuceTransactionHolder holder = new LettuceTransactionHolder();
            BlockingTransactionalRedisDataSourceImpl source = transactionalSource(conn, holder);
            LettuceResult.toBlocking(conn.watch(LettuceReactiveRedisDataSourceImpl.encodeKeys(watchedKeys)),
                    timeout);
            LettuceResult.toBlocking(conn.multi(), timeout);
            runTxBlock(conn, source, () -> tx.accept(source));
            return assembleResult(conn, holder, source.discarded());
        });
    }

    @Override
    public <I> OptimisticLockingTransactionResult<I> withTransaction(Function<RedisDataSource, I> preTx,
            BiConsumer<I, TransactionalRedisDataSource> tx, String... watchedKeys) {
        nonNull(preTx, "preTx");
        nonNull(tx, "tx");
        notNullOrEmpty(watchedKeys, "watchedKeys");
        doesNotContainNull(watchedKeys, "watchedKeys");
        LettuceConnection conn = acquire();
        return releasing(conn, () -> {
            LettuceTransactionHolder holder = new LettuceTransactionHolder();
            LettuceReactiveRedisDataSourceImpl pinnedReactive = reactive.pinnedTo(conn);
            BlockingTransactionalRedisDataSourceImpl source = new BlockingTransactionalRedisDataSourceImpl(
                    new LettuceReactiveTransactionalRedisDataSourceImpl(pinnedReactive, holder), timeout);

            LettuceResult.toBlocking(conn.watch(LettuceReactiveRedisDataSourceImpl.encodeKeys(watchedKeys)),
                    timeout);
            I input;
            try {
                input = preTx.apply(pinnedTo(pinnedReactive, timeout));
            } catch (RuntimeException e) {
                try {
                    LettuceResult.toBlocking(conn.unwatch(), timeout);
                } catch (RuntimeException e2) {
                    e.addSuppressed(e2);
                }
                throw e;
            }
            LettuceResult.toBlocking(conn.multi(), timeout);
            runTxBlock(conn, source, () -> tx.accept(input, source));
            if (source.discarded()) {
                return OptimisticLockingTransactionResultImpl.discarded(input);
            }
            io.lettuce.core.TransactionResult execResult = LettuceResult.toBlocking(conn.exec(), timeout);
            if (execResult == null || execResult.wasDiscarded()) {
                return OptimisticLockingTransactionResultImpl.discarded(input);
            }
            return holder.toOptimisticLockingResult(input, execResult);
        });
    }

    /**
     * Obtains the connection for a transaction: reuse the pinned outer connection when nested
     * inside {@code withConnection}, otherwise borrow one from the pool. On a cluster there is no
     * transaction to run, so this fails at once, before any connection is borrowed, with the message
     * of {@link LettuceConnection#TRANSACTIONS_NOT_SUPPORTED_ON_CLUSTER}.
     */
    private LettuceConnection acquire() {
        if (reactive.getConnection().isCluster()) {
            throw new UnsupportedOperationException(LettuceConnection.TRANSACTIONS_NOT_SUPPORTED_ON_CLUSTER);
        }
        return pinned ? reactive.getConnection() : reactive.acquireConnection(timeout);
    }

    /**
     * Runs {@code action} on {@code conn} and releases the connection afterwards, whatever the
     * outcome. The release never hides what the action did: when both fail, the caller gets the
     * action's exception with the release failure attached as suppressed, and a release failure
     * after a successful action is logged rather than thrown.
     */
    private <T> T releasing(LettuceConnection conn, Supplier<T> action) {
        Throwable actionFailure = null;
        try {
            return action.get();
        } catch (Throwable t) {
            actionFailure = t;
            throw t;
        } finally {
            release(conn, actionFailure);
        }
    }

    /**
     * Releases a connection. A pinned (reused) connection is left open for the outer scope to
     * release; a borrowed one is returned to the pool here.
     */
    private void release(LettuceConnection conn, Throwable actionFailure) {
        if (pinned) {
            return;
        }
        try {
            // Wait without cancelling on timeout: cancelling would only detach this thread from a
            // hand-over that must still run to completion, or the connection is lost to the pool.
            reactive.releaseConnection(conn).subscribeAsCompletionStage().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            Throwable releaseFailure = e instanceof ExecutionException && e.getCause() != null ? e.getCause() : e;
            if (actionFailure != null) {
                actionFailure.addSuppressed(releaseFailure);
            } else {
                LOGGER.warnf(releaseFailure, "Failed to release pooled Redis connection within %s", timeout);
            }
        }
    }

    private BlockingTransactionalRedisDataSourceImpl transactionalSource(LettuceConnection conn,
            LettuceTransactionHolder holder) {
        LettuceReactiveRedisDataSourceImpl pinnedReactive = reactive.pinnedTo(conn);
        return new BlockingTransactionalRedisDataSourceImpl(
                new LettuceReactiveTransactionalRedisDataSourceImpl(pinnedReactive, holder), timeout);
    }

    /**
     * Runs the user transaction block. On failure, issues {@code DISCARD} (unless the user already
     * discarded) and re-throws the original exception, attaching any {@code DISCARD} failure as
     * suppressed. Mirrors the Vert.x backend's abort path.
     */
    private void runTxBlock(LettuceConnection conn,
            BlockingTransactionalRedisDataSourceImpl source, Runnable block) {
        try {
            block.run();
        } catch (RuntimeException e) {
            if (!source.discarded()) {
                try {
                    LettuceResult.toBlocking(conn.discard(), timeout);
                } catch (RuntimeException e2) {
                    e.addSuppressed(e2);
                }
            }
            throw e;
        }
    }

    private TransactionResult assembleResult(LettuceConnection conn,
            LettuceTransactionHolder holder, boolean discarded) {
        if (discarded) {
            return TransactionResultImpl.DISCARDED;
        }
        io.lettuce.core.TransactionResult execResult = LettuceResult.toBlocking(conn.exec(), timeout);
        if (execResult == null || execResult.wasDiscarded()) {
            return TransactionResultImpl.DISCARDED;
        }
        return holder.toResult(execResult);
    }

    @Override
    public <K, V> ValueCommands<K, V> value(Class<K> redisKeyType, Class<V> valueType) {
        ReactiveValueCommands<K, V> r = reactive.value(redisKeyType, valueType);
        return new BlockingStringCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K, V> ValueCommands<K, V> value(TypeReference<K> redisKeyType, TypeReference<V> valueType) {
        ReactiveValueCommands<K, V> r = reactive.value(redisKeyType, valueType);
        return new BlockingStringCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K, V> StringCommands<K, V> string(Class<K> redisKeyType, Class<V> valueType) {
        ReactiveValueCommands<K, V> r = reactive.value(redisKeyType, valueType);
        return new BlockingStringCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K, F, V> HashCommands<K, F, V> hash(Class<K> redisKeyType, Class<F> typeOfField, Class<V> typeOfValue) {
        ReactiveHashCommands<K, F, V> r = reactive.hash(redisKeyType, typeOfField, typeOfValue);
        return new BlockingHashCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K, F, V> HashCommands<K, F, V> hash(TypeReference<K> redisKeyType, TypeReference<F> typeOfField,
            TypeReference<V> typeOfValue) {
        ReactiveHashCommands<K, F, V> r = reactive.hash(redisKeyType, typeOfField, typeOfValue);
        return new BlockingHashCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K, V> GeoCommands<K, V> geo(Class<K> redisKeyType, Class<V> memberType) {
        ReactiveGeoCommands<K, V> r = reactive.geo(redisKeyType, memberType);
        return new BlockingGeoCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K, V> GeoCommands<K, V> geo(TypeReference<K> redisKeyType, TypeReference<V> memberType) {
        ReactiveGeoCommands<K, V> r = reactive.geo(redisKeyType, memberType);
        return new BlockingGeoCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K> KeyCommands<K> key(Class<K> redisKeyType) {
        return new BlockingKeyCommandsImpl<>(this, reactive.key(redisKeyType), timeout);
    }

    @Override
    public <K> KeyCommands<K> key(TypeReference<K> redisKeyType) {
        return new BlockingKeyCommandsImpl<>(this, reactive.key(redisKeyType), timeout);
    }

    @Override
    public <K, V> SortedSetCommands<K, V> sortedSet(Class<K> redisKeyType, Class<V> valueType) {
        ReactiveSortedSetCommands<K, V> r = reactive.sortedSet(redisKeyType, valueType);
        return new BlockingSortedSetCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K, V> SortedSetCommands<K, V> sortedSet(TypeReference<K> redisKeyType, TypeReference<V> valueType) {
        ReactiveSortedSetCommands<K, V> r = reactive.sortedSet(redisKeyType, valueType);
        return new BlockingSortedSetCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K, V> SetCommands<K, V> set(Class<K> redisKeyType, Class<V> memberType) {
        ReactiveSetCommands<K, V> r = reactive.set(redisKeyType, memberType);
        return new BlockingSetCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K, V> SetCommands<K, V> set(TypeReference<K> redisKeyType, TypeReference<V> memberType) {
        ReactiveSetCommands<K, V> r = reactive.set(redisKeyType, memberType);
        return new BlockingSetCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K, V> ListCommands<K, V> list(Class<K> redisKeyType, Class<V> memberType) {
        ReactiveListCommands<K, V> r = reactive.list(redisKeyType, memberType);
        return new BlockingListCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K, V> ListCommands<K, V> list(TypeReference<K> redisKeyType, TypeReference<V> memberType) {
        ReactiveListCommands<K, V> r = reactive.list(redisKeyType, memberType);
        return new BlockingListCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K, V> HyperLogLogCommands<K, V> hyperloglog(Class<K> redisKeyType, Class<V> memberType) {
        ReactiveHyperLogLogCommands<K, V> r = reactive.hyperloglog(redisKeyType, memberType);
        return new BlockingHyperLogLogCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K, V> HyperLogLogCommands<K, V> hyperloglog(TypeReference<K> redisKeyType, TypeReference<V> memberType) {
        ReactiveHyperLogLogCommands<K, V> r = reactive.hyperloglog(redisKeyType, memberType);
        return new BlockingHyperLogLogCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K> BitMapCommands<K> bitmap(Class<K> redisKeyType) {
        ReactiveBitMapCommands<K> r = reactive.bitmap(redisKeyType);
        return new BlockingBitmapCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K> BitMapCommands<K> bitmap(TypeReference<K> redisKeyType) {
        ReactiveBitMapCommands<K> r = reactive.bitmap(redisKeyType);
        return new BlockingBitmapCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K, F, V> StreamCommands<K, F, V> stream(Class<K> redisKeyType, Class<F> fieldType, Class<V> valueType) {
        ReactiveStreamCommands<K, F, V> r = reactive.stream(redisKeyType, fieldType, valueType);
        return new BlockingStreamCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K, F, V> StreamCommands<K, F, V> stream(TypeReference<K> redisKeyType, TypeReference<F> fieldType,
            TypeReference<V> valueType) {
        ReactiveStreamCommands<K, F, V> r = reactive.stream(redisKeyType, fieldType, valueType);
        return new BlockingStreamCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <V> PubSubCommands<V> pubsub(Class<V> messageType) {
        ReactivePubSubCommands<V> r = reactive.pubsub(messageType);
        return new BlockingPubSubCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <V> PubSubCommands<V> pubsub(TypeReference<V> messageType) {
        ReactivePubSubCommands<V> r = reactive.pubsub(messageType);
        return new BlockingPubSubCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K> JsonCommands<K> json(Class<K> redisKeyType) {
        ReactiveJsonCommands<K> r = reactive.json(redisKeyType);
        return new BlockingJsonCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K> JsonCommands<K> json(TypeReference<K> redisKeyType) {
        ReactiveJsonCommands<K> r = reactive.json(redisKeyType);
        return new BlockingJsonCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K, V> BloomCommands<K, V> bloom(Class<K> redisKeyType, Class<V> valueType) {
        ReactiveBloomCommands<K, V> r = reactive.bloom(redisKeyType, valueType);
        return new BlockingBloomCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K, V> BloomCommands<K, V> bloom(TypeReference<K> redisKeyType, TypeReference<V> valueType) {
        ReactiveBloomCommands<K, V> r = reactive.bloom(redisKeyType, valueType);
        return new BlockingBloomCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K, V> CuckooCommands<K, V> cuckoo(Class<K> redisKeyType, Class<V> valueType) {
        ReactiveCuckooCommands<K, V> r = reactive.cuckoo(redisKeyType, valueType);
        return new BlockingCuckooCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K, V> CuckooCommands<K, V> cuckoo(TypeReference<K> redisKeyType, TypeReference<V> valueType) {
        ReactiveCuckooCommands<K, V> r = reactive.cuckoo(redisKeyType, valueType);
        return new BlockingCuckooCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K, V> CountMinCommands<K, V> countmin(Class<K> redisKeyType, Class<V> valueType) {
        ReactiveCountMinCommands<K, V> r = reactive.countmin(redisKeyType, valueType);
        return new BlockingCountMinCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K, V> CountMinCommands<K, V> countmin(TypeReference<K> redisKeyType, TypeReference<V> valueType) {
        ReactiveCountMinCommands<K, V> r = reactive.countmin(redisKeyType, valueType);
        return new BlockingCountMinCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K, V> TopKCommands<K, V> topk(Class<K> redisKeyType, Class<V> valueType) {
        ReactiveTopKCommands<K, V> r = reactive.topk(redisKeyType, valueType);
        return new BlockingTopKCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K, V> TopKCommands<K, V> topk(TypeReference<K> redisKeyType, TypeReference<V> valueType) {
        ReactiveTopKCommands<K, V> r = reactive.topk(redisKeyType, valueType);
        return new BlockingTopKCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K> GraphCommands<K> graph(Class<K> redisKeyType) {
        throw groupNotImplemented("graph");
    }

    @Override
    public <K> SearchCommands<K> search(Class<K> redisKeyType) {
        ReactiveSearchCommands<K> r = reactive.search(redisKeyType);
        return new BlockingSearchCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K> AutoSuggestCommands<K> autosuggest(Class<K> redisKeyType) {
        ReactiveAutoSuggestCommands<K> r = reactive.autosuggest(redisKeyType);
        return new BlockingAutoSuggestCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K> AutoSuggestCommands<K> autosuggest(TypeReference<K> redisKeyType) {
        ReactiveAutoSuggestCommands<K> r = reactive.autosuggest(redisKeyType);
        return new BlockingAutoSuggestCommandsImpl<>(this, r, timeout);
    }

    @Override
    public <K> TimeSeriesCommands<K> timeseries(Class<K> redisKeyType) {
        throw groupNotImplemented("timeseries");
    }

    @Override
    public <K> TimeSeriesCommands<K> timeseries(TypeReference<K> redisKeyType) {
        throw groupNotImplemented("timeseries");
    }

    private static UnsupportedOperationException groupNotImplemented(String group) {
        return new UnsupportedOperationException(
                "The '" + group + "' command group is not yet implemented on the Lettuce backend. "
                        + "Remove the quarkus-redis-client-lettuce extension to use the Vert.x backend.");
    }

}
