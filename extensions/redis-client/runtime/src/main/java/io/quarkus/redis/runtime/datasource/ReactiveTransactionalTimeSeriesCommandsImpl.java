package io.quarkus.redis.runtime.datasource;

import java.time.Duration;

import io.quarkus.redis.datasource.timeseries.AddArgs;
import io.quarkus.redis.datasource.timeseries.Aggregation;
import io.quarkus.redis.datasource.timeseries.AlterArgs;
import io.quarkus.redis.datasource.timeseries.CreateArgs;
import io.quarkus.redis.datasource.timeseries.Filter;
import io.quarkus.redis.datasource.timeseries.IncrementArgs;
import io.quarkus.redis.datasource.timeseries.MGetArgs;
import io.quarkus.redis.datasource.timeseries.MRangeArgs;
import io.quarkus.redis.datasource.timeseries.RangeArgs;
import io.quarkus.redis.datasource.timeseries.ReactiveTransactionalTimeSeriesCommands;
import io.quarkus.redis.datasource.timeseries.SeriesSample;
import io.quarkus.redis.datasource.timeseries.TimeSeriesRange;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.smallrye.mutiny.Uni;

public class ReactiveTransactionalTimeSeriesCommandsImpl<K> extends AbstractTransactionalCommands
        implements ReactiveTransactionalTimeSeriesCommands<K> {

    private final ReactiveTimeSeriesCommandsImpl<K> reactive;

    public ReactiveTransactionalTimeSeriesCommandsImpl(ReactiveTransactionalRedisDataSource ds,
            ReactiveTimeSeriesCommandsImpl<K> reactive, TransactionHolder tx) {
        super(ds, tx);
        this.reactive = reactive;
    }

    @Override
    public Uni<Void> tsCreate(K key, CreateArgs args) {
        this.tx.enqueue(x -> null);
        return this.reactive._tsCreate(key, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> tsCreate(K key) {
        this.tx.enqueue(x -> null);
        return this.reactive._tsCreate(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> tsAdd(K key, long timestamp, double value, AddArgs args) {
        this.tx.enqueue(x -> null);
        return this.reactive._tsAdd(key, timestamp, value, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> tsAdd(K key, long timestamp, double value) {
        this.tx.enqueue(x -> null);
        return this.reactive._tsAdd(key, timestamp, value).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> tsAdd(K key, double value) {
        this.tx.enqueue(x -> null);
        return this.reactive._tsAdd(key, value).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> tsAlter(K key, AlterArgs args) {
        this.tx.enqueue(x -> null);
        return this.reactive._tsAlter(key, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> tsCreateRule(K key, K destKey, Aggregation aggregation, Duration bucketDuration) {
        this.tx.enqueue(x -> null);
        return this.reactive._tsCreateRule(key, destKey, aggregation, bucketDuration).invoke(this::queuedOrDiscard)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> tsCreateRule(K key, K destKey, Aggregation aggregation, Duration bucketDuration,
            long alignTimestamp) {
        this.tx.enqueue(x -> null);
        return this.reactive._tsCreateRule(key, destKey, aggregation, bucketDuration, alignTimestamp)
                .invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> tsDecrBy(K key, double value) {
        this.tx.enqueue(x -> null);
        return this.reactive._tsDecrBy(key, value).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> tsDecrBy(K key, double value, IncrementArgs args) {
        this.tx.enqueue(x -> null);
        return this.reactive._tsDecrBy(key, value, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> tsDel(K key, long fromTimestamp, long toTimestamp) {
        this.tx.enqueue(x -> null);
        return this.reactive._tsDel(key, fromTimestamp, toTimestamp).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> tsDeleteRule(K key, K destKey) {
        this.tx.enqueue(x -> null);
        return this.reactive._tsDeleteRule(key, destKey).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> tsGet(K key) {
        this.tx.enqueue(reactive::decodeSample);
        return this.reactive._tsGet(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> tsGet(K key, boolean latest) {
        this.tx.enqueue(reactive::decodeSample);
        return this.reactive._tsGet(key, latest).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> tsIncrBy(K key, double value) {
        this.tx.enqueue(x -> null);
        return this.reactive._tsIncrBy(key, value).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> tsIncrBy(K key, double value, IncrementArgs args) {
        this.tx.enqueue(x -> null);
        return this.reactive._tsIncrBy(key, value, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> tsMAdd(SeriesSample<K>... samples) {
        this.tx.enqueue(x -> null);
        return this.reactive._tsMAdd(samples).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> tsMGet(MGetArgs args, Filter... filters) {
        this.tx.enqueue(reactive::decodeGroup);
        return this.reactive._tsMGet(args, filters).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> tsMGet(Filter... filters) {
        this.tx.enqueue(reactive::decodeGroup);
        return this.reactive._tsMGet(filters).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> tsMRange(TimeSeriesRange range, Filter... filters) {
        this.tx.enqueue(reactive::decodeGroup);
        return this.reactive._tsMRange(range, filters).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> tsMRange(TimeSeriesRange range, MRangeArgs args, Filter... filters) {
        this.tx.enqueue(reactive::decodeGroup);
        return this.reactive._tsMRange(range, args, filters).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> tsMRevRange(TimeSeriesRange range, Filter... filters) {
        this.tx.enqueue(reactive::decodeGroup);
        return this.reactive._tsMRevRange(range, filters).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> tsMRevRange(TimeSeriesRange range, MRangeArgs args, Filter... filters) {
        this.tx.enqueue(reactive::decodeGroup);
        return this.reactive._tsMRevRange(range, args, filters).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> tsQueryIndex(Filter... filters) {
        this.tx.enqueue(r -> reactive.marshaller.decodeAsList(r, reactive.keyType));
        return this.reactive._tsQueryIndex(filters).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> tsRange(K key, TimeSeriesRange range) {
        this.tx.enqueue(r -> reactive.marshaller.decodeAsList(r, reactive::decodeSample));
        return this.reactive._tsRange(key, range).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> tsRange(K key, TimeSeriesRange range, RangeArgs args) {
        this.tx.enqueue(r -> reactive.marshaller.decodeAsList(r, reactive::decodeSample));
        return this.reactive._tsRange(key, range, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> tsRevRange(K key, TimeSeriesRange range) {
        this.tx.enqueue(r -> reactive.marshaller.decodeAsList(r, reactive::decodeSample));
        return this.reactive._tsRevRange(key, range).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> tsRevRange(K key, TimeSeriesRange range, RangeArgs args) {
        this.tx.enqueue(r -> reactive.marshaller.decodeAsList(r, reactive::decodeSample));
        return this.reactive._tsRevRange(key, range, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }
}
