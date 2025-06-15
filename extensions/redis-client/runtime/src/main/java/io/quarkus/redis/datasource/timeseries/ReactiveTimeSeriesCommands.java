package io.quarkus.redis.datasource.timeseries;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.ReactiveRedisCommands;
import io.smallrye.common.annotation.Experimental;
import io.smallrye.mutiny.Uni;

/**
 * Allows executing commands from the {@code time series} group (requires the Redis Time Series module from Redis
 * stack). See <a href="https://redis.io/commands/?group=timeseries">the time series command list</a> for further
 * information about these commands.
 * <p>
 *
 * @param <K>
 *        the type of the key
 */
@Experimental("The commands from the time series group are experimental")
public interface ReactiveTimeSeriesCommands<K> extends ReactiveRedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/ts.create/">TS.CREATE</a>. Summary: Create a new time
     * series Group: time series
     *
     * @param key
     *        the key name for the time series must not be {@code null}
     * @param args
     *        the creation arguments.
     *
     * @return A uni emitting {@code null} when the operation completes
     **/
    Uni<Void> tsCreate(K key, CreateArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.create/">TS.CREATE</a>. Summary: Create a new time
     * series Group: time series
     *
     * @param key
     *        the key name for the time series must not be {@code null}
     *
     * @return A uni emitting {@code null} when the operation completes
     **/
    Uni<Void> tsCreate(K key);

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
     *
     * @return A uni emitting {@code null} when the operation completes
     **/
    Uni<Void> tsAdd(K key, long timestamp, double value, AddArgs args);

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
     *
     * @return A uni emitting {@code null} when the operation completes
     **/
    Uni<Void> tsAdd(K key, long timestamp, double value);

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
     *
     * @return A uni emitting {@code null} when the operation completes
     **/
    Uni<Void> tsAdd(K key, double value);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.add/">TS.ADD</a>. Summary: Append a sample to a time
     * series Group: time series
     * <p>
     * Unlike {@link #tsAdd(Object, long, double, AddArgs)}, set the timestamp according to the server clock.
     *
     * @param key
     *        the key name for the time series must not be {@code null}
     * @param value
     *        the numeric data value of the sample.
     * @param args
     *        the creation arguments.
     *
     * @return A uni emitting {@code null} when the operation completes
     */
    Uni<Void> tsAdd(K key, double value, AddArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.alter/">TS.ALTER</a>. Summary: Update the retention,
     * chunk size, duplicate policy, and labels of an existing time series Group: time series
     *
     * @param key
     *        the key name for the time series, must not be {@code null}
     * @param args
     *        the alter parameters, must not be {@code null}
     *
     * @return A uni emitting {@code null} when the operation completes
     **/
    Uni<Void> tsAlter(K key, AlterArgs args);

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
     *
     * @return A uni emitting {@code null} when the operation completes
     **/
    Uni<Void> tsCreateRule(K key, K destKey, Aggregation aggregation, Duration bucketDuration);

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
     *
     * @return A uni emitting {@code null} when the operation completes
     **/
    Uni<Void> tsCreateRule(K key, K destKey, Aggregation aggregation, Duration bucketDuration, long alignTimestamp);

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
     *
     * @return A uni emitting {@code null} when the operation completes
     **/
    Uni<Void> tsDecrBy(K key, double value);

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
     *
     * @return A uni emitting {@code null} when the operation completes
     **/
    Uni<Void> tsDecrBy(K key, double value, IncrementArgs args);

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
     *
     * @return A uni emitting {@code null} when the operation completes
     **/
    Uni<Void> tsDel(K key, long fromTimestamp, long toTimestamp);

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
     *
     * @return A uni emitting {@code null} when the operation completes
     **/
    Uni<Void> tsDeleteRule(K key, K destKey);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.get/">TS.GET</a>. Summary: Get the last sample Group:
     * time series
     * <p>
     *
     * @param key
     *        the key name for the time series. must not be {@code null}
     *
     * @return A uni emitting a {@code Sample}, i.e. a couple containing the timestamp and the value.
     **/
    Uni<Sample> tsGet(K key);

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
     *
     * @return A uni emitting a {@code Sample}, i.e. a couple containing the timestamp and the value.
     **/
    Uni<Sample> tsGet(K key, boolean latest);

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
     *
     * @return A uni emitting {@code null} when the operation completes
     **/
    Uni<Void> tsIncrBy(K key, double value);

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
     *
     * @return A uni emitting {@code null} when the operation completes
     **/
    Uni<Void> tsIncrBy(K key, double value, IncrementArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.madd/">TS.MADD</a>. Summary: Append new samples to one
     * or more time series Group: time series
     *
     * @param samples
     *        the set of samples to add to the time series.
     *
     * @return A uni emitting {@code null} when the operation completes
     **/
    Uni<Void> tsMAdd(SeriesSample<K>... samples);

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
     *
     * @return A uni emitting a Map of {@code String -> SampleGroup}, containing the matching sample grouped by key. The
     *         key is the string representation of the time series key.
     **/
    Uni<Map<String, SampleGroup>> tsMGet(MGetArgs args, Filter... filters);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.mget/">TS.MGET</a>. Summary: Get the last samples
     * matching a specific filter Group: time series
     * <p>
     *
     * @param filters
     *        the filters. Instanced are created using the static methods from {@link Filter}. Must not be
     *        {@code null}, or contain a {@code null} value.
     *
     * @return A uni emitting a Map of {@code String -> SampleGroup}, containing the matching sample grouped by key. The
     *         key is the string representation of the time series key.
     **/
    Uni<Map<String, SampleGroup>> tsMGet(Filter... filters);

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
     *
     * @return A uni emitting a Map of {@code String -> SampleGroup}, containing the matching sample grouped by key. The
     *         key is the string representation of the time series key.
     **/
    Uni<Map<String, SampleGroup>> tsMRange(TimeSeriesRange range, Filter... filters);

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
     *
     * @return A uni emitting a Map of {@code String -> SampleGroup}, containing the matching sample grouped by key. The
     *         key is the string representation of the time series key.
     **/
    Uni<Map<String, SampleGroup>> tsMRange(TimeSeriesRange range, MRangeArgs args, Filter... filters);

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
     *
     * @return A uni emitting a Map of {@code String -> SampleGroup}, containing the matching sample grouped by key. The
     *         key is the string representation of the time series key.
     **/
    Uni<Map<String, SampleGroup>> tsMRevRange(TimeSeriesRange range, Filter... filters);

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
     *
     * @return A uni emitting a Map of {@code String -> SampleGroup}, containing the matching sample grouped by key. The
     *         key is the string representation of the time series key.
     **/
    Uni<Map<String, SampleGroup>> tsMRevRange(TimeSeriesRange range, MRangeArgs args, Filter... filters);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.queryindex/">TS.QUERYINDEX</a>. Summary: Get all time
     * series keys matching a filter list Group: time series
     *
     * @param filters
     *        the filter, created from the {@link Filter} class. Must not be {@code null}, must not contain
     *        {@code null}
     *
     * @return A uni emitting the list of keys containing time series matching the filters
     **/
    Uni<List<K>> tsQueryIndex(Filter... filters);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.range/">TS.RANGE</a>. Summary: Query a range in forward
     * direction Group: time series
     * <p>
     *
     * @param key
     *        the key name for the time series, must not be {@code null}
     * @param range
     *        the range, must not be {@code null}
     *
     * @return A uni emitting the list of matching sample
     **/
    Uni<List<Sample>> tsRange(K key, TimeSeriesRange range);

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
     *
     * @return A uni emitting the list of matching sample
     **/
    Uni<List<Sample>> tsRange(K key, TimeSeriesRange range, RangeArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/ts.revrange/">TS.REVRANGE</a>. Summary: Query a range in
     * reverse direction Group: time series
     * <p>
     *
     * @param key
     *        the key name for the time series, must not be {@code null}
     * @param range
     *        the range, must not be {@code null}
     *
     * @return A uni emitting the list of matching sample
     **/
    Uni<List<Sample>> tsRevRange(K key, TimeSeriesRange range);

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
     *
     * @return A uni emitting the list of matching sample
     **/
    Uni<List<Sample>> tsRevRange(K key, TimeSeriesRange range, RangeArgs args);

}
