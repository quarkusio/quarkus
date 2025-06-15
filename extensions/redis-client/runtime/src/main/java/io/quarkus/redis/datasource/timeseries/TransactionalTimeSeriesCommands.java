package io.quarkus.redis.datasource.timeseries;

import java.time.Duration;

import io.quarkus.redis.datasource.TransactionalRedisCommands;

/**
 * Allows executing commands from the {@code time series} group (requires the Redis Time Series module from Redis
 * stack). See <a href="https://redis.io/commands/?group=timeseries">the time series command list</a> for further
 * information about these commands.
 * <p>
 * This API is intended to be used in a Redis transaction ({@code MULTI}), thus, all command methods return
 * {@code void}.
 *
 * @param <K>
 *        the type of the key
 */
public interface TransactionalTimeSeriesCommands<K> extends TransactionalRedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/ts.create/">TS.CREATE</a>. Summary: Create a new time
     * series Group: time series
     *
     * @param key
     *        the key name for the time series must not be {@code null}
     * @param args
     *        the creation arguments.
     */
    void tsCreate(K key, CreateArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.create/">TS.CREATE</a>. Summary: Create a new time
     * series Group: time series
     *
     * @param key
     *        the key name for the time series must not be {@code null}
     */
    void tsCreate(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.add/">TS.ADD</a>. Summary: Append a sample to a time
     * series Group: time series
     *
     * @param key
     *        the key name for the time series must not be {@code null}
     * @param timestamp
     *        the (long) UNIX sample timestamp in milliseconds or {@code -1} to set the timestamp according to the
     *        server clock.
     * @param value
     *        the numeric data value of the sample.
     * @param args
     *        the creation arguments.
     */
    void tsAdd(K key, long timestamp, double value, AddArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.add/">TS.ADD</a>. Summary: Append a sample to a time
     * series Group: time series
     *
     * @param key
     *        the key name for the time series, must not be {@code null}
     * @param timestamp
     *        the (long) UNIX sample timestamp in milliseconds
     * @param value
     *        the numeric data value of the sample
     */
    void tsAdd(K key, long timestamp, double value);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.add/">TS.ADD</a>. Summary: Append a sample to a time
     * series Group: time series
     * <p>
     * Unlike {@link #tsAdd(Object, long, double)}, set the timestamp according to the server clock.
     *
     * @param key
     *        the key name for the time series. must not be {@code null}
     * @param value
     *        the numeric data value of the sample, must not be {@code null}
     */
    void tsAdd(K key, double value);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.alter/">TS.ALTER</a>. Summary: Update the retention,
     * chunk size, duplicate policy, and labels of an existing time series Group: time series
     *
     * @param key
     *        the key name for the time series, must not be {@code null}
     * @param args
     *        the alter parameters, must not be {@code null}
     */
    void tsAlter(K key, AlterArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.createrule/">TS.CREATERULE</a>. Summary: Create a
     * compaction rule Group: time series
     *
     * @param key
     *        the key name for the time series, must not be {@code null}
     * @param destKey
     *        the key name for destination (compacted) time series. It must be created before TS.CREATERULE is
     *        called. Must not be {@code null}.
     * @param aggregation
     *        the aggregation function, must not be {@code null}
     * @param bucketDuration
     *        the duration of each bucket, must not be {@code null}
     */
    void tsCreateRule(K key, K destKey, Aggregation aggregation, Duration bucketDuration);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.createrule/">TS.CREATERULE</a>. Summary: Create a
     * compaction rule Group: time series
     *
     * @param key
     *        the key name for the time series, must not be {@code null}
     * @param destKey
     *        the key name for destination (compacted) time series. It must be created before TS.CREATERULE is
     *        called. Must not be {@code null}.
     * @param aggregation
     *        the aggregation function, must not be {@code null}
     * @param bucketDuration
     *        the duration of each bucket, must not be {@code null}
     * @param alignTimestamp
     *        when set, ensures that there is a bucket that starts exactly at alignTimestamp and aligns all other
     *        buckets accordingly. It is expressed in milliseconds. The default value is 0 aligned with the epoch.
     *        For example, if bucketDuration is 24 hours (24 * 3600 * 1000), setting alignTimestamp to 6 hours after
     *        the epoch (6 * 3600 * 1000) ensures that each bucket’s timeframe is [06:00 .. 06:00).
     */
    void tsCreateRule(K key, K destKey, Aggregation aggregation, Duration bucketDuration, long alignTimestamp);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.decrby/">TS.DECRBY</a>. Summary: Decrease the value of
     * the sample with the maximum existing timestamp, or create a new sample with a value equal to the value of the
     * sample with the maximum existing timestamp with a given decrement Group: time series
     * <p>
     *
     * @param key
     *        the key name for the time series. must not be {@code null}
     * @param value
     *        the numeric data value of the sample, must not be {@code null}
     */
    void tsDecrBy(K key, double value);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.decrby/">TS.DECRBY</a>. Summary: Decrease the value of
     * the sample with the maximum existing timestamp, or create a new sample with a value equal to the value of the
     * sample with the maximum existing timestamp with a given decrement Group: time series
     * <p>
     *
     * @param key
     *        the key name for the time series. must not be {@code null}
     * @param value
     *        the numeric data value of the sample, must not be {@code null}
     * @param args
     *        the extra command parameters
     */
    void tsDecrBy(K key, double value, IncrementArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.del/">TS.DEL</a>. Summary: Delete all samples between
     * two timestamps for a given time series Group: time series
     * <p>
     * The given timestamp interval is closed (inclusive), meaning that samples whose timestamp equals the fromTimestamp
     * or toTimestamp are also deleted.
     *
     * @param key
     *        the key name for the time series. must not be {@code null}
     * @param fromTimestamp
     *        the start timestamp for the range deletion.
     * @param toTimestamp
     *        the end timestamp for the range deletion.
     */
    void tsDel(K key, long fromTimestamp, long toTimestamp);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.deleterule/">TS.DELETERULE</a>. Summary: Delete a
     * compaction rule Group: time series
     * <p>
     *
     * @param key
     *        the key name for the time series. must not be {@code null}
     * @param destKey
     *        the key name for destination (compacted) time series. Note that the command does not delete the
     *        compacted series. Must not be {@code null}
     */
    void tsDeleteRule(K key, K destKey);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.get/">TS.GET</a>. Summary: Get the last sample Group:
     * time series
     * <p>
     *
     * @param key
     *        the key name for the time series. must not be {@code null}
     */
    void tsGet(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.get/">TS.GET</a>. Summary: Get the last sample Group:
     * time series
     * <p>
     *
     * @param key
     *        the key name for the time series. must not be {@code null}
     * @param latest
     *        used when a time series is a compaction. With LATEST set to {@code true}, TS.MRANGE also reports the
     *        compacted value of the latest possibly partial bucket, given that this bucket's start time falls
     *        within [fromTimestamp, toTimestamp]. Without LATEST, TS.MRANGE does not report the latest possibly
     *        partial bucket. When a time series is not a compaction, LATEST is ignored.
     */
    void tsGet(K key, boolean latest);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.incrby/">TS.INCRBY</a>. Summary: Increase the value of
     * the sample with the maximum existing timestamp, or create a new sample with a value equal to the value of the
     * sample with the maximum existing timestamp with a given increment. Group: time series
     * <p>
     *
     * @param key
     *        the key name for the time series. must not be {@code null}
     * @param value
     *        the numeric data value of the sample, must not be {@code null}
     */
    void tsIncrBy(K key, double value);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.incrby/">TS.INCRBY</a>. Summary: Increase the value of
     * the sample with the maximum existing timestamp, or create a new sample with a value equal to the value of the
     * sample with the maximum existing timestamp with a given increment. Group: time series
     * <p>
     *
     * @param key
     *        the key name for the time series. must not be {@code null}
     * @param value
     *        the numeric data value of the sample, must not be {@code null}
     * @param args
     *        the extra command parameters
     */
    void tsIncrBy(K key, double value, IncrementArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.madd/">TS.MADD</a>. Summary: Append new samples to one
     * or more time series Group: time series
     *
     * @param samples
     *        the set of samples to add to the time series.
     */
    void tsMAdd(SeriesSample<K>... samples);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.mget/">TS.MGET</a>. Summary: Get the last samples
     * matching a specific filter Group: time series
     * <p>
     *
     * @param args
     *        the extra command parameter, must not be {@code null}
     * @param filters
     *        the filters. Instanced are created using the static methods from {@link Filter}. Must not be
     *        {@code null}, or contain a {@code null} value.
     */
    void tsMGet(MGetArgs args, Filter... filters);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.mget/">TS.MGET</a>. Summary: Get the last samples
     * matching a specific filter Group: time series
     * <p>
     *
     * @param filters
     *        the filters. Instanced are created using the static methods from {@link Filter}. Must not be
     *        {@code null}, or contain a {@code null} value.
     */
    void tsMGet(Filter... filters);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.mrange/">TS.MRANGE</a>. Summary: Query a range across
     * multiple time series by filters in forward direction Group: time series
     * <p>
     *
     * @param range
     *        the range, must not be {@code null}
     * @param filters
     *        the filters. Instanced are created using the static methods from {@link Filter}. Must not be
     *        {@code null}, or contain a {@code null} value.
     */
    void tsMRange(TimeSeriesRange range, Filter... filters);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.mrange/">TS.MRANGE</a>. Summary: Query a range across
     * multiple time series by filters in forward direction Group: time series
     * <p>
     *
     * @param range
     *        the range, must not be {@code null}
     * @param args
     *        the extra command parameters
     * @param filters
     *        the filters. Instanced are created using the static methods from {@link Filter}. Must not be
     *        {@code null}, or contain a {@code null} value.
     */
    void tsMRange(TimeSeriesRange range, MRangeArgs args, Filter... filters);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.mrevrange/">TS.MREVRANGE</a>. Summary: Query a range
     * across multiple time series by filters in reverse direction Group: time series
     * <p>
     *
     * @param range
     *        the range, must not be {@code null}
     * @param filters
     *        the filters. Instanced are created using the static methods from {@link Filter}. Must not be
     *        {@code null}, or contain a {@code null} value.
     */
    void tsMRevRange(TimeSeriesRange range, Filter... filters);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.mrevrange/">TS.MREVRANGE</a>. Summary: Query a range
     * across multiple time series by filters in reverse direction Group: time series
     * <p>
     *
     * @param range
     *        the range, must not be {@code null}
     * @param args
     *        the extra command parameters
     * @param filters
     *        the filters. Instanced are created using the static methods from {@link Filter}. Must not be
     *        {@code null}, or contain a {@code null} value.
     */
    void tsMRevRange(TimeSeriesRange range, MRangeArgs args, Filter... filters);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.queryindex/">TS.QUERYINDEX</a>. Summary: Get all time
     * series keys matching a filter list Group: time series
     *
     * @param filters
     *        the filter, created from the {@link Filter} class. Must not be {@code null}, must not contain
     *        {@code null}
     */
    void tsQueryIndex(Filter... filters);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.range/">TS.RANGE</a>. Summary: Query a range in forward
     * direction Group: time series
     * <p>
     *
     * @param key
     *        the key name for the time series, must not be {@code null}
     * @param range
     *        the range, must not be {@code null}
     */
    void tsRange(K key, TimeSeriesRange range);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.range/">TS.RANGE</a>. Summary: Query a range in forward
     * direction Group: time series
     * <p>
     *
     * @param key
     *        the key name for the time series, must not be {@code null}
     * @param range
     *        the range, must not be {@code null}
     * @param args
     *        the extra command parameters
     */
    void tsRange(K key, TimeSeriesRange range, RangeArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.revrange/">TS.REVRANGE</a>. Summary: Query a range in
     * reverse direction Group: time series
     * <p>
     *
     * @param key
     *        the key name for the time series, must not be {@code null}
     * @param range
     *        the range, must not be {@code null}
     */
    void tsRevRange(K key, TimeSeriesRange range);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.revrange/">TS.REVRANGE</a>. Summary: Query a range in
     * reverse direction Group: time series
     * <p>
     *
     * @param key
     *        the key name for the time series, must not be {@code null}
     * @param range
     *        the range, must not be {@code null}
     * @param args
     *        the extra command parameters
     */
    void tsRevRange(K key, TimeSeriesRange range, RangeArgs args);
}
