package io.quarkus.redis.lettuce.runtime.internal.datasource;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.quarkus.redis.datasource.autosuggest.ReactiveTransactionalAutoSuggestCommands;
import io.quarkus.redis.datasource.bitmap.ReactiveTransactionalBitMapCommands;
import io.quarkus.redis.datasource.bloom.ReactiveTransactionalBloomCommands;
import io.quarkus.redis.datasource.countmin.ReactiveTransactionalCountMinCommands;
import io.quarkus.redis.datasource.cuckoo.ReactiveTransactionalCuckooCommands;
import io.quarkus.redis.datasource.geo.ReactiveTransactionalGeoCommands;
import io.quarkus.redis.datasource.graph.ReactiveTransactionalGraphCommands;
import io.quarkus.redis.datasource.hash.ReactiveTransactionalHashCommands;
import io.quarkus.redis.datasource.hyperloglog.ReactiveTransactionalHyperLogLogCommands;
import io.quarkus.redis.datasource.json.ReactiveTransactionalJsonCommands;
import io.quarkus.redis.datasource.keys.ReactiveTransactionalKeyCommands;
import io.quarkus.redis.datasource.list.ReactiveTransactionalListCommands;
import io.quarkus.redis.datasource.search.ReactiveTransactionalSearchCommands;
import io.quarkus.redis.datasource.set.ReactiveTransactionalSetCommands;
import io.quarkus.redis.datasource.sortedset.ReactiveTransactionalSortedSetCommands;
import io.quarkus.redis.datasource.stream.ReactiveTransactionalStreamCommands;
import io.quarkus.redis.datasource.string.ReactiveTransactionalStringCommands;
import io.quarkus.redis.datasource.timeseries.ReactiveTransactionalTimeSeriesCommands;
import io.quarkus.redis.datasource.topk.ReactiveTransactionalTopKCommands;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.quarkus.redis.datasource.value.ReactiveTransactionalValueCommands;
import io.quarkus.redis.lettuce.runtime.internal.LettuceCommand;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnection;
import io.quarkus.redis.lettuce.runtime.internal.LettuceResult;
import io.quarkus.redis.lettuce.runtime.internal.autosuggest.LettuceReactiveAutoSuggestCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.autosuggest.LettuceReactiveTransactionalAutoSuggestCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.bitmap.LettuceReactiveBitMapCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.bitmap.LettuceReactiveTransactionalBitMapCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.countmin.LettuceReactiveCountMinCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.countmin.LettuceReactiveTransactionalCountMinCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.geo.LettuceReactiveGeoCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.geo.LettuceReactiveTransactionalGeoCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.hash.LettuceReactiveHashCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.hash.LettuceReactiveTransactionalHashCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.hyperloglog.LettuceReactiveHyperLogLogCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.hyperloglog.LettuceReactiveTransactionalHyperLogLogCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.json.LettuceReactiveJsonCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.json.LettuceReactiveTransactionalJsonCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.key.LettuceReactiveKeyCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.key.LettuceReactiveTransactionalKeyCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.list.LettuceReactiveListCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.list.LettuceReactiveTransactionalListCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.set.LettuceReactiveSetCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.set.LettuceReactiveTransactionalSetCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.sortedset.LettuceReactiveSortedSetCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.sortedset.LettuceReactiveTransactionalSortedSetCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.value.LettuceReactiveTransactionalValueCommandsImpl;
import io.quarkus.redis.lettuce.runtime.internal.value.LettuceReactiveValueCommandsImpl;
import io.smallrye.mutiny.Uni;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.impl.CommandMap;

/**
 * Lettuce-backed implementation of {@link ReactiveTransactionalRedisDataSource}.
 * <p>
 * Wraps the pinned {@link LettuceReactiveRedisDataSourceImpl} on which {@code MULTI} has already
 * been issued, together with the {@link LettuceTransactionHolder} that captures every queued
 * command. Command-group accessors return Lettuce transactional command groups that enqueue their
 * {@code RedisFuture}s on the same pinned connection; the orchestrator in
 * {@code LettuceReactiveRedisDataSourceImpl#withTransaction(...)} assembles the typed
 * {@code TransactionResult} once {@code EXEC} has run.
 * <p>
 * Only the {@code value}, {@code key} and {@code execute(...)} surfaces are wired — every other
 * command group throws {@link UnsupportedOperationException}, mirroring the shape of the
 * non-transactional Lettuce data source.
 */
public class LettuceReactiveTransactionalRedisDataSourceImpl implements ReactiveTransactionalRedisDataSource {

    private final LettuceReactiveRedisDataSourceImpl reactive;
    private final LettuceConnection connection;
    private final LettuceTransactionHolder tx;

    public LettuceReactiveTransactionalRedisDataSourceImpl(LettuceReactiveRedisDataSourceImpl reactive,
            LettuceTransactionHolder tx) {
        this.reactive = nonNull(reactive, "reactive");
        this.connection = reactive.getConnection();
        this.tx = nonNull(tx, "tx");
    }

    @Override
    public Uni<Void> discard() {
        return LettuceResult.toUni(connection::discard)
                .invoke(tx::discard)
                .replaceWithVoid();
    }

    @Override
    public boolean discarded() {
        return tx.discarded();
    }

    @Override
    public <K, V> ReactiveTransactionalValueCommands<K, V> value(Class<K> redisKeyType, Class<V> valueType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(valueType, "valueType");
        LettuceReactiveValueCommandsImpl<K, V> reactiveValue = (LettuceReactiveValueCommandsImpl<K, V>) reactive
                .value(redisKeyType, valueType);
        return new LettuceReactiveTransactionalValueCommandsImpl<>(this, reactiveValue, tx);
    }

    @Override
    public <K, V> ReactiveTransactionalStringCommands<K, V> string(Class<K> redisKeyType, Class<V> valueType) {
        throw groupNotImplemented("string");
    }

    @Override
    public <K> ReactiveTransactionalKeyCommands<K> key(Class<K> redisKeyType) {
        nonNull(redisKeyType, "redisKeyType");
        LettuceReactiveKeyCommandsImpl<K> reactiveKey = (LettuceReactiveKeyCommandsImpl<K>) reactive
                .key(redisKeyType);
        return new LettuceReactiveTransactionalKeyCommandsImpl<>(this, reactiveKey, tx);
    }

    @Override
    public Uni<Void> execute(String command, String... args) {
        nonNull(command, "command");
        return enqueueRaw(LettuceReactiveRedisDataSourceImpl.resolve(command),
                LettuceReactiveRedisDataSourceImpl.rawArgs(CommandMap.getKnownCommand(command), args));
    }

    @Override
    public Uni<Void> execute(Command command, String... args) {
        nonNull(command, "command");
        return enqueueRaw(LettuceReactiveRedisDataSourceImpl.resolve(command.toString()),
                LettuceReactiveRedisDataSourceImpl.rawArgs(command, args));
    }

    @Override
    public <K, F, V> ReactiveTransactionalHashCommands<K, F, V> hash(Class<K> redisKeyType, Class<F> typeOfField,
            Class<V> typeOfValue) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(typeOfField, "typeOfField");
        nonNull(typeOfValue, "typeOfValue");
        LettuceReactiveHashCommandsImpl<K, F, V> reactiveHash = (LettuceReactiveHashCommandsImpl<K, F, V>) reactive
                .hash(redisKeyType, typeOfField, typeOfValue);
        return new LettuceReactiveTransactionalHashCommandsImpl<>(this, reactiveHash, tx);
    }

    @Override
    public <K, V> ReactiveTransactionalGeoCommands<K, V> geo(Class<K> redisKeyType, Class<V> memberType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(memberType, "memberType");
        LettuceReactiveGeoCommandsImpl<K, V> reactiveGeo = (LettuceReactiveGeoCommandsImpl<K, V>) reactive
                .geo(redisKeyType, memberType);
        return new LettuceReactiveTransactionalGeoCommandsImpl<>(this, reactiveGeo, tx);
    }

    @Override
    public <K, V> ReactiveTransactionalSortedSetCommands<K, V> sortedSet(Class<K> redisKeyType, Class<V> valueType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(valueType, "valueType");
        LettuceReactiveSortedSetCommandsImpl<K, V> reactiveSortedSet = (LettuceReactiveSortedSetCommandsImpl<K, V>) reactive
                .sortedSet(redisKeyType, valueType);
        return new LettuceReactiveTransactionalSortedSetCommandsImpl<>(this, reactiveSortedSet, tx);
    }

    @Override
    public <K, V> ReactiveTransactionalSetCommands<K, V> set(Class<K> redisKeyType, Class<V> memberType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(memberType, "memberType");
        LettuceReactiveSetCommandsImpl<K, V> reactiveSet = (LettuceReactiveSetCommandsImpl<K, V>) reactive
                .set(redisKeyType, memberType);
        return new LettuceReactiveTransactionalSetCommandsImpl<>(this, reactiveSet, tx);
    }

    @Override
    public <K, V> ReactiveTransactionalListCommands<K, V> list(Class<K> redisKeyType, Class<V> memberType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(memberType, "memberType");
        LettuceReactiveListCommandsImpl<K, V> reactiveList = (LettuceReactiveListCommandsImpl<K, V>) reactive
                .list(redisKeyType, memberType);
        return new LettuceReactiveTransactionalListCommandsImpl<>(this, reactiveList, tx);
    }

    @Override
    public <K, V> ReactiveTransactionalHyperLogLogCommands<K, V> hyperloglog(Class<K> redisKeyType, Class<V> memberType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(memberType, "memberType");
        LettuceReactiveHyperLogLogCommandsImpl<K, V> reactiveHyperLogLog = (LettuceReactiveHyperLogLogCommandsImpl<K, V>) reactive
                .hyperloglog(redisKeyType, memberType);
        return new LettuceReactiveTransactionalHyperLogLogCommandsImpl<>(this, reactiveHyperLogLog, tx);
    }

    @Override
    public <K> ReactiveTransactionalBitMapCommands<K> bitmap(Class<K> redisKeyType) {
        nonNull(redisKeyType, "redisKeyType");
        LettuceReactiveBitMapCommandsImpl<K> reactiveBitMap = (LettuceReactiveBitMapCommandsImpl<K>) reactive
                .bitmap(redisKeyType);
        return new LettuceReactiveTransactionalBitMapCommandsImpl<>(this, reactiveBitMap, tx);
    }

    @Override
    public <K, F, V> ReactiveTransactionalStreamCommands<K, F, V> stream(Class<K> redisKeyType, Class<F> typeOfField,
            Class<V> typeOfValue) {
        throw groupNotImplemented("stream");
    }

    @Override
    public <K> ReactiveTransactionalJsonCommands<K> json(Class<K> redisKeyType) {
        nonNull(redisKeyType, "redisKeyType");
        LettuceReactiveJsonCommandsImpl<K> reactiveJson = (LettuceReactiveJsonCommandsImpl<K>) reactive
                .json(redisKeyType);
        return new LettuceReactiveTransactionalJsonCommandsImpl<>(this, reactiveJson, tx);
    }

    @Override
    public <K, V> ReactiveTransactionalBloomCommands<K, V> bloom(Class<K> redisKeyType, Class<V> valueType) {
        throw groupNotImplemented("bloom");
    }

    @Override
    public <K, V> ReactiveTransactionalCuckooCommands<K, V> cuckoo(Class<K> redisKeyType, Class<V> valueType) {
        throw groupNotImplemented("cuckoo");
    }

    @Override
    public <K, V> ReactiveTransactionalCountMinCommands<K, V> countmin(Class<K> redisKeyType, Class<V> valueType) {
        nonNull(redisKeyType, "redisKeyType");
        nonNull(valueType, "valueType");
        LettuceReactiveCountMinCommandsImpl<K, V> reactiveCountMin = (LettuceReactiveCountMinCommandsImpl<K, V>) reactive
                .countmin(redisKeyType, valueType);
        return new LettuceReactiveTransactionalCountMinCommandsImpl<>(this, reactiveCountMin, tx);
    }

    @Override
    public <K, V> ReactiveTransactionalTopKCommands<K, V> topk(Class<K> redisKeyType, Class<V> valueType) {
        throw groupNotImplemented("topk");
    }

    @Override
    public <K> ReactiveTransactionalGraphCommands<K> graph(Class<K> redisKeyType) {
        throw groupNotImplemented("graph");
    }

    @Override
    public <K> ReactiveTransactionalSearchCommands search(Class<K> redisKeyType) {
        throw groupNotImplemented("search");
    }

    @Override
    public <K> ReactiveTransactionalAutoSuggestCommands<K> autosuggest(Class<K> redisKeyType) {
        nonNull(redisKeyType, "redisKeyType");
        LettuceReactiveAutoSuggestCommandsImpl<K> reactiveAutoSuggest = (LettuceReactiveAutoSuggestCommandsImpl<K>) reactive
                .autosuggest(redisKeyType);
        return new LettuceReactiveTransactionalAutoSuggestCommandsImpl<>(this, reactiveAutoSuggest, tx);
    }

    @Override
    public <K> ReactiveTransactionalTimeSeriesCommands<K> timeseries(Class<K> redisKeyType) {
        throw groupNotImplemented("timeseries");
    }

    private Uni<Void> enqueueRaw(ProtocolKeyword type, CommandArgs<byte[], byte[]> commandArgs) {
        LettuceVertxResponseOutput<byte[], byte[]> output = new LettuceVertxResponseOutput<>(ByteArrayCodec.INSTANCE);
        return tx.enqueue(LettuceCommand.of(() -> connection.async().dispatch(type, output, commandArgs),
                ignored -> output.toVertxResponse()));
    }

    private static UnsupportedOperationException groupNotImplemented(String group) {
        return new UnsupportedOperationException(
                "The '" + group + "' transactional command group is not yet implemented on the Lettuce backend. "
                        + "Remove the quarkus-redis-client-lettuce extension to use the Vert.x backend.");
    }
}
