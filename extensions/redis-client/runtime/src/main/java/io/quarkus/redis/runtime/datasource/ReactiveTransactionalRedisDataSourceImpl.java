package io.quarkus.redis.runtime.datasource;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.Arrays;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
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
        return new ReactiveTransactionalHashCommandsImpl<>(this,
                (ReactiveHashCommandsImpl<K, F, V>) this.reactive.hash(redisKeyType, typeOfField, typeOfValue),
                tx);
    }

    @Override
    public <K, V> ReactiveTransactionalGeoCommands<K, V> geo(Class<K> redisKeyType, Class<V> memberType) {
        return new ReactiveTransactionalGeoCommandsImpl<>(this,
                (ReactiveGeoCommandsImpl<K, V>) this.reactive.geo(redisKeyType, memberType), tx);
    }

    @Override
    public <K, V> ReactiveTransactionalSortedSetCommands<K, V> sortedSet(Class<K> redisKeyType, Class<V> valueType) {
        return new ReactiveTransactionalSortedSetCommandsImpl<>(this,
                (ReactiveSortedSetCommandsImpl<K, V>) this.reactive.sortedSet(redisKeyType, valueType), tx);
    }

    @Override
    public <K, V> ReactiveTransactionalValueCommands<K, V> value(Class<K> redisKeyType, Class<V> valueType) {
        return new ReactiveTransactionalStringCommandsImpl<>(this,
                (ReactiveStringCommandsImpl<K, V>) this.reactive.value(redisKeyType, valueType), tx);
    }

    @Override
    public <K, V> ReactiveTransactionalStringCommands<K, V> string(Class<K> redisKeyType, Class<V> valueType) {
        return new ReactiveTransactionalStringCommandsImpl<>(this,
                (ReactiveStringCommandsImpl<K, V>) this.reactive.value(redisKeyType, valueType), tx);
    }

    @Override
    public <K, V> ReactiveTransactionalSetCommands<K, V> set(Class<K> redisKeyType, Class<V> memberType) {
        return new ReactiveTransactionalSetCommandsImpl<>(this,
                (ReactiveSetCommandsImpl<K, V>) this.reactive.set(redisKeyType, memberType), tx);
    }

    @Override
    public <K, V> ReactiveTransactionalListCommands<K, V> list(Class<K> redisKeyType, Class<V> memberType) {
        return new ReactiveTransactionalListCommandsImpl<>(this,
                (ReactiveListCommandsImpl<K, V>) this.reactive.list(redisKeyType, memberType), tx);
    }

    @Override
    public <K, V> ReactiveTransactionalHyperLogLogCommands<K, V> hyperloglog(Class<K> redisKeyType, Class<V> memberType) {
        return new ReactiveTransactionalHyperLogLogCommandsImpl<>(this,
                (ReactiveHyperLogLogCommandsImpl<K, V>) this.reactive.hyperloglog(redisKeyType, memberType), tx);
    }

    @Override
    public <K> ReactiveTransactionalBitMapCommands<K> bitmap(Class<K> redisKeyType) {
        return new ReactiveTransactionalBitMapCommandsImpl<>(this,
                (ReactiveBitMapCommandsImpl<K>) this.reactive.bitmap(redisKeyType), tx);
    }

    @Override
    public <K, F, V> ReactiveTransactionalStreamCommands<K, F, V> stream(Class<K> redisKeyType, Class<F> typeOfField,
            Class<V> typeOfValue) {
        return new ReactiveTransactionalStreamCommandsImpl<>(this,
                (ReactiveStreamCommandsImpl<K, F, V>) this.reactive.stream(redisKeyType, typeOfField, typeOfValue), tx);
    }

    @Override
    public <K> ReactiveTransactionalJsonCommands<K> json(Class<K> redisKeyType) {
        return new ReactiveTransactionalJsonCommandsImpl<>(this,
                (ReactiveJsonCommandsImpl<K>) this.reactive.json(redisKeyType), tx);
    }

    @Override
    public <K, V> ReactiveTransactionalBloomCommands<K, V> bloom(Class<K> redisKeyType, Class<V> valueType) {
        return new ReactiveTransactionalBloomCommandsImpl<>(this,
                (ReactiveBloomCommandsImpl<K, V>) this.reactive.bloom(redisKeyType), tx);
    }

    @Override
    public <K, V> ReactiveTransactionalCuckooCommands<K, V> cuckoo(Class<K> redisKeyType, Class<V> valueType) {
        return new ReactiveTransactionalCuckooCommandsImpl<>(this,
                (ReactiveCuckooCommandsImpl<K, V>) this.reactive.cuckoo(redisKeyType, valueType), tx);
    }

    @Override
    public <K, V> ReactiveTransactionalCountMinCommands<K, V> countmin(Class<K> redisKeyType, Class<V> valueType) {
        return new ReactiveTransactionalCountMinCommandsImpl<>(this,
                (ReactiveCountMinCommandsImpl<K, V>) this.reactive.countmin(redisKeyType, valueType), tx);
    }

    @Override
    public <K, V> ReactiveTransactionalTopKCommands<K, V> topk(Class<K> redisKeyType, Class<V> valueType) {
        return new ReactiveTransactionalTopKCommandsImpl<>(this,
                (ReactiveTopKCommandsImpl<K, V>) this.reactive.topk(redisKeyType, valueType), tx);
    }

    @Override
    public <K> ReactiveTransactionalGraphCommands<K> graph(Class<K> redisKeyType) {
        return new ReactiveTransactionalGraphCommandsImpl<>(this,
                (ReactiveGraphCommandsImpl<K>) this.reactive.graph(redisKeyType), tx);
    }

    @Override
    public <K> ReactiveTransactionalKeyCommands<K> key(Class<K> redisKeyType) {
        return new ReactiveTransactionalKeyCommandsImpl<>(this,
                (ReactiveKeyCommandsImpl<K>) this.reactive.key(redisKeyType), tx);
    }

    @Override
    public <K> ReactiveTransactionalSearchCommands search(Class<K> redisKeyType) {
        return new ReactiveTransactionalSearchCommandsImpl<>(this,
                (ReactiveSearchCommandsImpl<K>) this.reactive.search(redisKeyType), tx);
    }

    @Override
    public <K> ReactiveTransactionalAutoSuggestCommands<K> autosuggest(Class<K> redisKeyType) {
        return new ReactiveTransactionalAutoSuggestCommandsImpl<>(this,
                (ReactiveAutoSuggestCommandsImpl<K>) this.reactive.autosuggest(redisKeyType), tx);
    }

    @Override
    public <K> ReactiveTransactionalTimeSeriesCommands<K> timeseries(Class<K> redisKeyType) {
        return new ReactiveTransactionalTimeSeriesCommandsImpl<>(this,
                (ReactiveTimeSeriesCommandsImpl<K>) this.reactive.timeseries(redisKeyType), tx);
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
                        return Uni.createFrom()
                                .failure(new IllegalStateException("Unable to enqueue command into the current transaction"));
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
