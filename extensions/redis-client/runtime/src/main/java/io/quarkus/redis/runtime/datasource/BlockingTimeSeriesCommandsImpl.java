package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.timeseries.AddArgs;
import io.quarkus.redis.datasource.timeseries.Aggregation;
import io.quarkus.redis.datasource.timeseries.AlterArgs;
import io.quarkus.redis.datasource.timeseries.CreateArgs;
import io.quarkus.redis.datasource.timeseries.Filter;
import io.quarkus.redis.datasource.timeseries.IncrementArgs;
import io.quarkus.redis.datasource.timeseries.MGetArgs;
import io.quarkus.redis.datasource.timeseries.MRangeArgs;
import io.quarkus.redis.datasource.timeseries.RangeArgs;
import io.quarkus.redis.datasource.timeseries.ReactiveTimeSeriesCommands;
import io.quarkus.redis.datasource.timeseries.Sample;
import io.quarkus.redis.datasource.timeseries.SampleGroup;
import io.quarkus.redis.datasource.timeseries.SeriesSample;
import io.quarkus.redis.datasource.timeseries.TimeSeriesCommands;
import io.quarkus.redis.datasource.timeseries.TimeSeriesRange;

public class BlockingTimeSeriesCommandsImpl<K> extends AbstractRedisCommandGroup implements TimeSeriesCommands<K> {

    private final ReactiveTimeSeriesCommands<K> reactive;

    public BlockingTimeSeriesCommandsImpl(RedisDataSource ds, ReactiveTimeSeriesCommands<K> reactive,
            Duration timeout) {
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
    public void tsAdd(K key, double value, AddArgs args) {
        reactive.tsAdd(key, value, args).await().atMost(timeout);
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
    public Sample tsGet(K key) {
        return reactive.tsGet(key).await().atMost(timeout);
    }

    @Override
    public Sample tsGet(K key, boolean latest) {
        return reactive.tsGet(key, latest).await().atMost(timeout);
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
    public Map<String, SampleGroup> tsMGet(MGetArgs args, Filter... filters) {
        return reactive.tsMGet(args, filters).await().atMost(timeout);
    }

    @Override
    public Map<String, SampleGroup> tsMGet(Filter... filters) {
        return reactive.tsMGet(filters).await().atMost(timeout);
    }

    @Override
    public Map<String, SampleGroup> tsMRange(TimeSeriesRange range, Filter... filters) {
        return reactive.tsMRange(range, filters).await().atMost(timeout);
    }

    @Override
    public Map<String, SampleGroup> tsMRange(TimeSeriesRange range, MRangeArgs args, Filter... filters) {
        return reactive.tsMRange(range, args, filters).await().atMost(timeout);
    }

    @Override
    public Map<String, SampleGroup> tsMRevRange(TimeSeriesRange range, Filter... filters) {
        return reactive.tsMRevRange(range, filters).await().atMost(timeout);
    }

    @Override
    public Map<String, SampleGroup> tsMRevRange(TimeSeriesRange range, MRangeArgs args, Filter... filters) {
        return reactive.tsMRevRange(range, args, filters).await().atMost(timeout);
    }

    @Override
    public List<K> tsQueryIndex(Filter... filters) {
        return reactive.tsQueryIndex(filters).await().atMost(timeout);
    }

    @Override
    public List<Sample> tsRange(K key, TimeSeriesRange range) {
        return reactive.tsRange(key, range).await().atMost(timeout);
    }

    @Override
    public List<Sample> tsRange(K key, TimeSeriesRange range, RangeArgs args) {
        return reactive.tsRange(key, range, args).await().atMost(timeout);
    }

    @Override
    public List<Sample> tsRevRange(K key, TimeSeriesRange range) {
        return reactive.tsRevRange(key, range).await().atMost(timeout);
    }

    @Override
    public List<Sample> tsRevRange(K key, TimeSeriesRange range, RangeArgs args) {
        return reactive.tsRevRange(key, range, args).await().atMost(timeout);
    }
}
