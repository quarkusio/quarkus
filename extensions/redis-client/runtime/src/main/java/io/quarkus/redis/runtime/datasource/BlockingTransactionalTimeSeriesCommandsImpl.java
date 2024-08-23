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
import io.quarkus.redis.datasource.timeseries.TransactionalTimeSeriesCommands;
import io.quarkus.redis.datasource.transactions.TransactionalRedisDataSource;

public class BlockingTransactionalTimeSeriesCommandsImpl<K> extends AbstractTransactionalRedisCommandGroup
        implements TransactionalTimeSeriesCommands<K> {

    private final ReactiveTransactionalTimeSeriesCommands<K> reactive;

    public BlockingTransactionalTimeSeriesCommandsImpl(TransactionalRedisDataSource ds,
            ReactiveTransactionalTimeSeriesCommands<K> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public void tsCreate(K key, CreateArgs args) {
        reactive.tsCreate(key, args).await().atMost(timeout);
    }

    @Override
    public void tsCreate(K key) {
        reactive.tsCreate(key).await().atMost(timeout);
    }

    @Override
    public void tsAdd(K key, long timestamp, double value, AddArgs args) {
        reactive.tsAdd(key, timestamp, value, args).await().atMost(timeout);
    }

    @Override
    public void tsAdd(K key, long timestamp, double value) {
        reactive.tsAdd(key, timestamp, value).await().atMost(timeout);
    }

    @Override
    public void tsAdd(K key, double value) {
        reactive.tsAdd(key, value).await().atMost(timeout);
    }

    @Override
    public void tsAlter(K key, AlterArgs args) {
        reactive.tsAlter(key, args).await().atMost(timeout);
    }

    @Override
    public void tsCreateRule(K key, K destKey, Aggregation aggregation, Duration bucketDuration) {
        reactive.tsCreateRule(key, destKey, aggregation, bucketDuration).await().atMost(timeout);
    }

    @Override
    public void tsCreateRule(K key, K destKey, Aggregation aggregation, Duration bucketDuration, long alignTimestamp) {
        reactive.tsCreateRule(key, destKey, aggregation, bucketDuration, alignTimestamp).await().atMost(timeout);
    }

    @Override
    public void tsDecrBy(K key, double value) {
        reactive.tsDecrBy(key, value).await().atMost(timeout);
    }

    @Override
    public void tsDecrBy(K key, double value, IncrementArgs args) {
        reactive.tsDecrBy(key, value, args).await().atMost(timeout);
    }

    @Override
    public void tsDel(K key, long fromTimestamp, long toTimestamp) {
        reactive.tsDel(key, fromTimestamp, toTimestamp).await().atMost(timeout);
    }

    @Override
    public void tsDeleteRule(K key, K destKey) {
        reactive.tsDeleteRule(key, destKey).await().atMost(timeout);
    }

    @Override
    public void tsGet(K key) {
        reactive.tsGet(key).await().atMost(timeout);
    }

    @Override
    public void tsGet(K key, boolean latest) {
        reactive.tsGet(key, latest).await().atMost(timeout);
    }

    @Override
    public void tsIncrBy(K key, double value) {
        reactive.tsIncrBy(key, value).await().atMost(timeout);
    }

    @Override
    public void tsIncrBy(K key, double value, IncrementArgs args) {
        reactive.tsIncrBy(key, value, args).await().atMost(timeout);
    }

    @Override
    public void tsMAdd(SeriesSample<K>... samples) {
        reactive.tsMAdd(samples).await().atMost(timeout);
    }

    @Override
    public void tsMGet(MGetArgs args, Filter... filters) {
        reactive.tsMGet(args, filters).await().atMost(timeout);
    }

    @Override
    public void tsMGet(Filter... filters) {
        reactive.tsMGet(filters).await().atMost(timeout);
    }

    @Override
    public void tsMRange(TimeSeriesRange range, Filter... filters) {
        reactive.tsMRange(range, filters).await().atMost(timeout);
    }

    @Override
    public void tsMRange(TimeSeriesRange range, MRangeArgs args, Filter... filters) {
        reactive.tsMRange(range, args, filters).await().atMost(timeout);
    }

    @Override
    public void tsMRevRange(TimeSeriesRange range, Filter... filters) {
        reactive.tsMRevRange(range, filters).await().atMost(timeout);
    }

    @Override
    public void tsMRevRange(TimeSeriesRange range, MRangeArgs args, Filter... filters) {
        reactive.tsMRevRange(range, args, filters).await().atMost(timeout);
    }

    @Override
    public void tsQueryIndex(Filter... filters) {
        reactive.tsQueryIndex(filters).await().atMost(timeout);
    }

    @Override
    public void tsRange(K key, TimeSeriesRange range) {
        reactive.tsRange(key, range).await().atMost(timeout);
    }

    @Override
    public void tsRange(K key, TimeSeriesRange range, RangeArgs args) {
        reactive.tsRange(key, range, args).await().atMost(timeout);
    }

    @Override
    public void tsRevRange(K key, TimeSeriesRange range) {
        reactive.tsRevRange(key, range).await().atMost(timeout);
    }

    @Override
    public void tsRevRange(K key, TimeSeriesRange range, RangeArgs args) {
        reactive.tsRevRange(key, range, args).await().atMost(timeout);
    }
}
