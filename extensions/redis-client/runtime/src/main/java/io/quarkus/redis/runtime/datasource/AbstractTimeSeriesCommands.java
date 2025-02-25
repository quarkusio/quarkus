package io.quarkus.redis.runtime.datasource;

import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;

import java.lang.reflect.Type;
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
import io.quarkus.redis.datasource.timeseries.SeriesSample;
import io.quarkus.redis.datasource.timeseries.TimeSeriesRange;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Response;

public class AbstractTimeSeriesCommands<K> extends AbstractRedisCommands {

    AbstractTimeSeriesCommands(RedisCommandExecutor redis, Type k) {
        super(redis, new Marshaller(k));
    }

    Uni<Response> _tsCreate(K key, CreateArgs args) {
        nonNull(key, "key");
        nonNull(args, "args");

        RedisCommand cmd = RedisCommand.of(Command.TS_CREATE).put(marshaller.encode(key)).putArgs(args);
        return execute(cmd);
    }

    Uni<Response> _tsCreate(K key) {
        nonNull(key, "key");

        RedisCommand cmd = RedisCommand.of(Command.TS_CREATE).put(marshaller.encode(key));
        return execute(cmd);
    }

    Uni<Response> _tsAdd(K key, long timestamp, double value, AddArgs args) {
        nonNull(key, "key");
        positive(timestamp, "timestamp");
        nonNull(args, "args");

        RedisCommand cmd = RedisCommand.of(Command.TS_ADD).put(marshaller.encode(key)).put(timestamp).put(value).putArgs(args);

        return execute(cmd);
    }

    Uni<Response> _tsAdd(K key, long timestamp, double value) {
        nonNull(key, "key");
        positive(timestamp, "timestamp");

        RedisCommand cmd = RedisCommand.of(Command.TS_ADD).put(marshaller.encode(key)).put(timestamp).put(value);
        return execute(cmd);
    }

    Uni<Response> _tsAdd(K key, double value) {
        nonNull(key, "key");

        RedisCommand cmd = RedisCommand.of(Command.TS_ADD).put(marshaller.encode(key)).put("*").put(value);
        return execute(cmd);
    }

    Uni<Response> _tsAdd(K key, double value, AddArgs args) {
        nonNull(key, "key");
        nonNull(args, "args");
        RedisCommand cmd = RedisCommand.of(Command.TS_ADD).put(marshaller.encode(key)).put("*").put(value).putArgs(args);
        return execute(cmd);
    }

    Uni<Response> _tsAlter(K key, AlterArgs args) {
        nonNull(key, "key");
        nonNull(args, "args");

        RedisCommand cmd = RedisCommand.of(Command.TS_ALTER).put(marshaller.encode(key)).putArgs(args);
        return execute(cmd);
    }

    Uni<Response> _tsCreateRule(K key, K destKey, Aggregation aggregation, Duration bucketDuration) {
        nonNull(key, "key");
        nonNull(destKey, "destKey");
        nonNull(aggregation, "aggregation");
        nonNull(bucketDuration, "bucketDuration");

        RedisCommand cmd = RedisCommand.of(Command.TS_CREATERULE).put(marshaller.encode(key)).put(marshaller.encode(destKey))
                .put("AGGREGATION").put(aggregation.toString()).put(bucketDuration.toMillis());
        return execute(cmd);
    }

    Uni<Response> _tsCreateRule(K key, K destKey, Aggregation aggregation, Duration bucketDuration, long alignTimestamp) {
        nonNull(key, "key");
        nonNull(destKey, "destKey");
        nonNull(aggregation, "aggregation");
        nonNull(bucketDuration, "bucketDuration");
        positive(alignTimestamp, "alignTimestamp");

        RedisCommand cmd = RedisCommand.of(Command.TS_CREATERULE)
                .put(marshaller.encode(key))
                .put(marshaller.encode(destKey))
                .put("AGGREGATION")
                .put(aggregation.toString()).put(bucketDuration.toMillis())
                .put(alignTimestamp);
        return execute(cmd);
    }

    Uni<Response> _tsDecrBy(K key, double value) {

        nonNull(key, "key");

        RedisCommand cmd = RedisCommand.of(Command.TS_DECRBY).put(marshaller.encode(key)).put(value);
        return execute(cmd);
    }

    Uni<Response> _tsDecrBy(K key, double value, IncrementArgs args) {
        nonNull(key, "key");
        nonNull(args, "args");

        RedisCommand cmd = RedisCommand.of(Command.TS_DECRBY).put(marshaller.encode(key)).put(value).putArgs(args);
        return execute(cmd);
    }

    Uni<Response> _tsDel(K key, long fromTimestamp, long toTimestamp) {
        nonNull(key, "key");
        positive(fromTimestamp, "fromTimestamp");
        positive(toTimestamp, "toTimestamp");

        RedisCommand cmd = RedisCommand.of(Command.TS_DEL).put(marshaller.encode(key)).put(fromTimestamp).put(toTimestamp);
        return execute(cmd);
    }

    Uni<Response> _tsDeleteRule(K key, K destKey) {
        nonNull(key, "key");
        nonNull(destKey, "destKey");

        RedisCommand cmd = RedisCommand.of(Command.TS_DELETERULE).put(marshaller.encode(key)).put(marshaller.encode(destKey));
        return execute(cmd);
    }

    Uni<Response> _tsGet(K key) {
        nonNull(key, "key");
        RedisCommand cmd = RedisCommand.of(Command.TS_GET).put(marshaller.encode(key));
        return execute(cmd);
    }

    Uni<Response> _tsGet(K key, boolean latest) {
        nonNull(key, "key");
        RedisCommand cmd = RedisCommand.of(Command.TS_GET).put(marshaller.encode(key));
        if (latest) {
            cmd.put("LATEST");
        }
        return execute(cmd);
    }

    Uni<Response> _tsIncrBy(K key, double value) {
        nonNull(key, "key");

        RedisCommand cmd = RedisCommand.of(Command.TS_INCRBY).put(marshaller.encode(key)).put(value);
        return execute(cmd);
    }

    Uni<Response> _tsIncrBy(K key, double value, IncrementArgs args) {
        nonNull(key, "key");
        nonNull(args, "args");

        RedisCommand cmd = RedisCommand.of(Command.TS_INCRBY).put(marshaller.encode(key)).put(value).putArgs(args);
        return execute(cmd);
    }

    Uni<Response> _tsMAdd(SeriesSample<K>... samples) {
        doesNotContainNull(samples, "samples");
        if (samples.length == 0) {
            return Uni.createFrom().failure(new IllegalArgumentException("`samples` must not be empty"));
        }

        RedisCommand cmd = RedisCommand.of(Command.TS_MADD);
        for (SeriesSample<K> sample : samples) {
            cmd.put(marshaller.encode(sample.key));
            if (sample.timestamp == Long.MAX_VALUE) {
                cmd.put("*");
            } else {
                cmd.put(sample.timestamp);
            }
            cmd.put(sample.value);
        }
        return execute(cmd);
    }

    Uni<Response> _tsMGet(MGetArgs args, Filter... filters) {
        nonNull(args, "args");
        doesNotContainNull(filters, "filters");
        if (filters.length == 0) {
            return Uni.createFrom().failure(new IllegalArgumentException("`filters` must not be empty"));
        }
        RedisCommand cmd = RedisCommand.of(Command.TS_MGET).putArgs(args);
        cmd.put("FILTER");
        for (Filter filter : filters) {
            cmd.put(filter.toString());
        }

        return execute(cmd);
    }

    Uni<Response> _tsMGet(Filter... filters) {
        doesNotContainNull(filters, "filters");
        if (filters.length == 0) {
            return Uni.createFrom().failure(new IllegalArgumentException("`filters` must not be empty"));
        }
        RedisCommand cmd = RedisCommand.of(Command.TS_MGET);
        cmd.put("FILTER");
        for (Filter filter : filters) {
            cmd.put(filter.toString());
        }

        return execute(cmd);
    }

    Uni<Response> _tsMRange(TimeSeriesRange range, Filter... filters) {

        nonNull(range, "range");
        doesNotContainNull(filters, "filters");
        if (filters.length == 0) {
            return Uni.createFrom().failure(new IllegalArgumentException("`filters` must not be empty"));
        }

        RedisCommand cmd = RedisCommand.of(Command.TS_MRANGE).putAll(range.toArgs());
        cmd.put("FILTER");
        for (Filter filter : filters) {
            cmd.put(filter.toString());
        }

        return execute(cmd);
    }

    Uni<Response> _tsMRange(TimeSeriesRange range, MRangeArgs args, Filter... filters) {

        nonNull(range, "range");
        nonNull(args, "args");
        doesNotContainNull(filters, "filters");
        if (filters.length == 0) {
            return Uni.createFrom().failure(new IllegalArgumentException("`filters` must not be empty"));
        }

        RedisCommand cmd = RedisCommand.of(Command.TS_MRANGE).putAll(range.toArgs()).putArgs(args);
        cmd.put("FILTER");
        for (Filter filter : filters) {
            cmd.put(filter.toString());
        }
        cmd.putAll(args.getGroupByClauseArgs());

        return execute(cmd);
    }

    Uni<Response> _tsMRevRange(TimeSeriesRange range, Filter... filters) {

        nonNull(range, "range");
        doesNotContainNull(filters, "filters");

        RedisCommand cmd = RedisCommand.of(Command.TS_MREVRANGE).putAll(range.toArgs());
        cmd.put("FILTER");
        for (Filter filter : filters) {
            cmd.put(filter.toString());
        }

        return execute(cmd);
    }

    Uni<Response> _tsMRevRange(TimeSeriesRange range, MRangeArgs args, Filter... filters) {

        nonNull(args, "args");
        nonNull(range, "range");
        doesNotContainNull(filters, "filters");
        if (filters.length == 0) {
            return Uni.createFrom().failure(new IllegalArgumentException("`filters` must not be empty"));
        }

        RedisCommand cmd = RedisCommand.of(Command.TS_MREVRANGE).putAll(range.toArgs()).putArgs(args);

        cmd.put("FILTER");
        for (Filter filter : filters) {
            cmd.put(filter.toString());
        }
        return execute(cmd);
    }

    Uni<Response> _tsQueryIndex(Filter... filters) {

        doesNotContainNull(filters, "filters");
        if (filters.length == 0) {
            return Uni.createFrom().failure(new IllegalArgumentException("`filters` must not be empty"));
        }

        RedisCommand cmd = RedisCommand.of(Command.TS_QUERYINDEX);
        for (Filter filter : filters) {
            cmd.put(filter.toString());
        }

        return execute(cmd);
    }

    Uni<Response> _tsRange(K key, TimeSeriesRange range) {

        nonNull(key, "key");
        nonNull(range, "range");

        RedisCommand cmd = RedisCommand.of(Command.TS_RANGE).put(marshaller.encode(key)).putAll(range.toArgs());
        return execute(cmd);
    }

    Uni<Response> _tsRange(K key, TimeSeriesRange range, RangeArgs args) {

        nonNull(key, "key");
        nonNull(range, "range");
        nonNull(args, "args");

        RedisCommand cmd = RedisCommand.of(Command.TS_RANGE).put(marshaller.encode(key)).putAll(range.toArgs()).putArgs(args);
        return execute(cmd);
    }

    Uni<Response> _tsRevRange(K key, TimeSeriesRange range) {

        nonNull(key, "key");
        nonNull(range, "range");

        RedisCommand cmd = RedisCommand.of(Command.TS_REVRANGE).put(marshaller.encode(key)).putAll(range.toArgs());
        return execute(cmd);
    }

    Uni<Response> _tsRevRange(K key, TimeSeriesRange range, RangeArgs args) {

        nonNull(key, "key");
        nonNull(range, "range");
        nonNull(args, "args");

        RedisCommand cmd = RedisCommand.of(Command.TS_REVRANGE).put(marshaller.encode(key)).putAll(range.toArgs())
                .putArgs(args);
        return execute(cmd);
    }
}
