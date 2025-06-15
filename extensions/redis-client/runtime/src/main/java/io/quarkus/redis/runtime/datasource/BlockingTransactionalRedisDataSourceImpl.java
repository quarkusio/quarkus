package io.quarkus.redis.runtime.datasource;

import java.time.Duration;

import io.quarkus.redis.datasource.autosuggest.TransactionalAutoSuggestCommands;
import io.quarkus.redis.datasource.bitmap.TransactionalBitMapCommands;
import io.quarkus.redis.datasource.bloom.TransactionalBloomCommands;
import io.quarkus.redis.datasource.countmin.TransactionalCountMinCommands;
import io.quarkus.redis.datasource.cuckoo.TransactionalCuckooCommands;
import io.quarkus.redis.datasource.geo.TransactionalGeoCommands;
import io.quarkus.redis.datasource.graph.TransactionalGraphCommands;
import io.quarkus.redis.datasource.hash.TransactionalHashCommands;
import io.quarkus.redis.datasource.hyperloglog.TransactionalHyperLogLogCommands;
import io.quarkus.redis.datasource.json.TransactionalJsonCommands;
import io.quarkus.redis.datasource.keys.TransactionalKeyCommands;
import io.quarkus.redis.datasource.list.TransactionalListCommands;
import io.quarkus.redis.datasource.search.TransactionalSearchCommands;
import io.quarkus.redis.datasource.set.TransactionalSetCommands;
import io.quarkus.redis.datasource.sortedset.TransactionalSortedSetCommands;
import io.quarkus.redis.datasource.stream.TransactionalStreamCommands;
import io.quarkus.redis.datasource.string.TransactionalStringCommands;
import io.quarkus.redis.datasource.timeseries.TransactionalTimeSeriesCommands;
import io.quarkus.redis.datasource.topk.TransactionalTopKCommands;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.quarkus.redis.datasource.transactions.TransactionalRedisDataSource;
import io.quarkus.redis.datasource.value.TransactionalValueCommands;
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
        return new BlockingTransactionalHashCommandsImpl<>(this, reactive.hash(redisKeyType, typeOfField, typeOfValue),
                timeout);
    }

    @Override
    public <K, V> TransactionalGeoCommands<K, V> geo(Class<K> redisKeyType, Class<V> memberType) {
        return new BlockingTransactionalGeoCommandsImpl<>(this, reactive.geo(redisKeyType, memberType), timeout);
    }

    @Override
    public <K> TransactionalKeyCommands<K> key(Class<K> redisKeyType) {
        return new BlockingTransactionalKeyCommandsImpl<>(this, reactive.key(redisKeyType), timeout);
    }

    @Override
    public <K, V> TransactionalSetCommands<K, V> set(Class<K> redisKeyType, Class<V> memberType) {
        return new BlockingTransactionalSetCommandsImpl<>(this, reactive.set(redisKeyType, memberType), timeout);
    }

    @Override
    public <K, V> TransactionalSortedSetCommands<K, V> sortedSet(Class<K> redisKeyType, Class<V> valueType) {
        return new BlockingTransactionalSortedSetCommandsImpl<>(this, reactive.sortedSet(redisKeyType, valueType),
                timeout);
    }

    @Override
    public <K, V> TransactionalStringCommands<K, V> string(Class<K> redisKeyType, Class<V> valueType) {
        return new BlockingTransactionalStringCommandsImpl<>(this, reactive.value(redisKeyType, valueType), timeout);
    }

    @Override
    public <K, V> TransactionalValueCommands<K, V> value(Class<K> redisKeyType, Class<V> valueType) {
        return new BlockingTransactionalStringCommandsImpl<>(this, reactive.value(redisKeyType, valueType), timeout);
    }

    @Override
    public <K, V> TransactionalListCommands<K, V> list(Class<K> redisKeyType, Class<V> memberType) {
        return new BlockingTransactionalListCommandsImpl<>(this, reactive.list(redisKeyType, memberType), timeout);
    }

    @Override
    public <K, V> TransactionalHyperLogLogCommands<K, V> hyperloglog(Class<K> redisKeyType, Class<V> memberType) {
        return new BlockingTransactionalHyperLogLogCommandsImpl<>(this, reactive.hyperloglog(redisKeyType, memberType),
                timeout);
    }

    @Override
    public <K> TransactionalBitMapCommands<K> bitmap(Class<K> redisKeyType) {
        return new BlockingTransactionalBitMapCommandsImpl<>(this, reactive.bitmap(redisKeyType), timeout);
    }

    @Override
    public <K, F, V> TransactionalStreamCommands<K, F, V> stream(Class<K> redisKeyType, Class<F> typeOfField,
            Class<V> typeOfValue) {
        return new BlockingTransactionalStreamCommandsImpl<>(this,
                reactive.stream(redisKeyType, typeOfField, typeOfValue), timeout);
    }

    @Override
    public <K> TransactionalJsonCommands<K> json(Class<K> redisKeyType) {
        return new BlockingTransactionalJsonCommandsImpl<>(this, reactive.json(redisKeyType), timeout);
    }

    @Override
    public <K, V> TransactionalBloomCommands<K, V> bloom(Class<K> redisKeyType, Class<V> valueType) {
        return new BlockingTransactionalBloomCommandsImpl<>(this, reactive.bloom(redisKeyType, valueType), timeout);
    }

    @Override
    public <K, V> TransactionalCuckooCommands<K, V> cuckoo(Class<K> redisKeyType, Class<V> valueType) {
        return new BlockingTransactionalCuckooCommandsImpl<>(this, reactive.cuckoo(redisKeyType, valueType), timeout);
    }

    @Override
    public <K, V> TransactionalCountMinCommands<K, V> countmin(Class<K> redisKeyType, Class<V> valueType) {
        return new BlockingTransactionalCountMinCommandsImpl<>(this, reactive.countmin(redisKeyType, valueType),
                timeout);
    }

    @Override
    public <K, V> TransactionalTopKCommands<K, V> topk(Class<K> redisKeyType, Class<V> valueType) {
        return new BlockingTransactionalTopKCommandsImpl<>(this, reactive.topk(redisKeyType, valueType), timeout);
    }

    @Override
    public <K> TransactionalGraphCommands<K> graph(Class<K> redisKeyType) {
        return new BlockingTransactionalGraphCommandsImpl<>(this, reactive.graph(redisKeyType), timeout);
    }

    @Override
    public <K> TransactionalSearchCommands search(Class<K> redisKeyType) {
        return new BlockingTransactionalSearchCommandsImpl(this, reactive.search(redisKeyType), timeout);
    }

    @Override
    public <K> TransactionalAutoSuggestCommands<K> autosuggest(Class<K> redisKeyType) {
        return new BlockingTransactionalAutoSuggestCommandsImpl<>(this, reactive.autosuggest(redisKeyType), timeout);
    }

    @Override
    public <K> TransactionalTimeSeriesCommands<K> timeseries(Class<K> redisKeyType) {
        return new BlockingTransactionalTimeSeriesCommandsImpl<>(this, reactive.timeseries(redisKeyType), timeout);
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
