
package io.quarkus.redis.runtime.datasource;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;
import static io.smallrye.mutiny.helpers.ParameterValidation.validate;
import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.ScanArgs;
import io.quarkus.redis.datasource.list.KeyValue;
import io.quarkus.redis.datasource.sortedset.Range;
import io.quarkus.redis.datasource.sortedset.ReactiveZScanCursor;
import io.quarkus.redis.datasource.sortedset.ScoreRange;
import io.quarkus.redis.datasource.sortedset.ScoredValue;
import io.quarkus.redis.datasource.sortedset.ZAddArgs;
import io.quarkus.redis.datasource.sortedset.ZAggregateArgs;
import io.quarkus.redis.datasource.sortedset.ZRangeArgs;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Response;
import io.vertx.redis.client.ResponseType;

class AbstractSortedSetCommands<K, V> extends ReactiveSortable<K, V> {

    protected final Type typeOfValue;
    protected final Type typeOfKey;

    protected static final ZAggregateArgs DEFAULT_INSTANCE_AGG = new ZAggregateArgs();
    protected static final ZRangeArgs DEFAULT_INSTANCE_RANGE = new ZRangeArgs();

    AbstractSortedSetCommands(RedisCommandExecutor redis, Type k, Type v) {
        super(redis, new Marshaller(k, v), v);
        this.typeOfValue = v;
        this.typeOfKey = k;
    }

    Uni<Response> _zadd(K key, double score, V value) {
        return _zadd(key, new ZAddArgs(), score, value);
    }

    Uni<Response> _zadd(K key, Map<V, Double> items) {
        return _zadd(key, new ZAddArgs(), items);
    }

    Uni<Response> _zadd(K key, ScoredValue<V>... items) {
        return _zadd(key, new ZAddArgs(), items);
    }

    Uni<Response> _zadd(K key, ZAddArgs args, double score, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        nonNull(args, "args");
        String s = getScoreAsString(score);

        return execute(RedisCommand.of(Command.ZADD).put(marshaller.encode(key))
                .putArgs(args)
                .put(s).put(marshaller.encode(value)));
    }

    Uni<Response> _zadd(K key, ZAddArgs args, Map<V, Double> items) {
        nonNull(key, "key");
        nonNull(items, "items");
        nonNull(args, "args");

        RedisCommand cmd = RedisCommand.of(Command.ZADD).put(marshaller.encode(key)).putArgs(args);
        for (Map.Entry<V, Double> entry : items.entrySet()) {
            nonNull(entry.getValue(), "value from items");
            String s = getScoreAsString(entry.getValue());
            cmd.put(s).put(marshaller.encode(entry.getKey()));
        }
        return execute(cmd);
    }

    Uni<Response> _zadd(K key, ZAddArgs args, ScoredValue<V>... items) {
        nonNull(key, "key");
        nonNull(items, "items");
        nonNull(args, "args");

        RedisCommand cmd = RedisCommand.of(Command.ZADD).put(marshaller.encode(key)).putArgs(args);
        for (ScoredValue<V> item : items) {
            nonNull(item.value, "value from scored value");
            String s = getScoreAsString(item.score);
            byte[] m = marshaller.encode(item.value);
            cmd.put(s).put(m);
        }
        return execute(cmd);
    }

    Uni<Response> _zaddincr(K key, double score, V value) {
        return _zaddincr(key, new ZAddArgs(), score, value);
    }

    Uni<Response> _zaddincr(K key, ZAddArgs args, double score, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        nonNull(args, "args");
        String s = getScoreAsString(score);

        return execute(RedisCommand.of(Command.ZADD).put(marshaller.encode(key))
                .putArgs(args)
                .put("INCR")
                .put(s)
                .put(marshaller.encode(value)));
    }

    Uni<Response> _zcard(K key) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.ZCARD).put(marshaller.encode(key)));
    }

    Uni<Response> _zcount(K key, ScoreRange<Double> range) {
        nonNull(key, "key");
        nonNull(range, "range");
        return execute(RedisCommand.of(Command.ZCOUNT).put(marshaller.encode(key))
                .put(range.getLowerBound()).put(range.getUpperBound()));
    }

    Uni<Response> _zdiff(K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length < 2) {
            return Uni.createFrom().failure(new IllegalArgumentException("Need at least two keys"));
        }
        RedisCommand cmd = RedisCommand.of(Command.ZDIFF)
                .put(keys.length)
                .putAll(marshaller.encode(keys));
        return execute(cmd);
    }

    Uni<Response> _zdiffWithScores(K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length < 2) {
            return Uni.createFrom().failure(new IllegalArgumentException("Need at least two keys"));
        }

        RedisCommand cmd = RedisCommand.of(Command.ZDIFF)
                .put(keys.length);

        for (K key : keys) {
            cmd.put(marshaller.encode(key));
        }

        cmd.put("WITHSCORES");

        return execute(cmd);
    }

    Uni<Response> _zdiffstore(K destination, K... keys) {
        nonNull(destination, "destination");
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length < 2) {
            return Uni.createFrom().failure(new IllegalArgumentException("Need at least two keys"));
        }
        RedisCommand cmd = RedisCommand.of(Command.ZDIFFSTORE)
                .put(marshaller.encode(destination))
                .put(keys.length);

        for (K key : keys) {
            cmd.put(marshaller.encode(key));
        }

        return execute(cmd);
    }

    Uni<Response> _zincrby(K key, double increment, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        String s = getScoreAsString(increment);

        return execute(RedisCommand.of(Command.ZINCRBY).put(marshaller.encode(key))
                .put(s).put(marshaller.encode(value)));
    }

    Uni<Response> _zinter(ZAggregateArgs args, K... keys) {
        nonNull(args, "args");
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length < 2) {
            return Uni.createFrom().failure(new IllegalArgumentException("Need at least two keys"));
        }
        RedisCommand cmd = RedisCommand.of(Command.ZINTER)
                .put(keys.length);
        for (K k : keys) {
            cmd.put(marshaller.encode(k));
        }
        cmd.putArgs(args);

        return execute(cmd);
    }

    Uni<Response> _zinter(K... keys) {
        return _zinter(DEFAULT_INSTANCE_AGG, keys);
    }

    Uni<Response> _zinterWithScores(ZAggregateArgs arguments, K... keys) {
        nonNull(arguments, "arguments");
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length < 2) {
            return Uni.createFrom().failure(new IllegalArgumentException("Need at least two keys"));
        }
        RedisCommand cmd = RedisCommand.of(Command.ZINTER)
                .put(keys.length);
        for (K key : keys) {
            cmd.put(marshaller.encode(key));
        }
        cmd
                .putArgs(arguments)
                .put("WITHSCORES");
        return execute(cmd);
    }

    Uni<Response> _zinterWithScores(K... keys) {
        return _zinterWithScores(DEFAULT_INSTANCE_AGG, keys);
    }

    Uni<Response> _zintercard(K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length < 2) {
            return Uni.createFrom().failure(new IllegalArgumentException("Need at least two keys"));
        }
        RedisCommand cmd = RedisCommand.of(Command.ZINTERCARD)
                .put(keys.length);

        for (K key : keys) {
            cmd.put(marshaller.encode(key));
        }

        return execute(cmd);
    }

    Uni<Response> _zintercard(long limit, K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length < 2) {
            return Uni.createFrom().failure(new IllegalArgumentException("Need at least two keys"));
        }
        positive(limit, "limit");
        RedisCommand cmd = RedisCommand.of(Command.ZINTERCARD)
                .put(keys.length);

        for (K key : keys) {
            cmd.put(marshaller.encode(key));
        }

        if (limit != 0) {
            cmd.put("LIMIT").put(limit);
        }
        return execute(cmd);
    }

    Uni<Response> _zinterstore(K destination, ZAggregateArgs arguments, K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length < 2) {
            return Uni.createFrom().failure(new IllegalArgumentException("Need at least two keys"));
        }
        nonNull(arguments, "arguments");
        nonNull(destination, "destination");
        RedisCommand cmd = RedisCommand.of(Command.ZINTERSTORE)
                .put(marshaller.encode(destination))
                .put(keys.length)
                .putAll(marshaller.encode(keys))
                .putArgs(arguments);

        return execute(cmd);
    }

    @SafeVarargs
    final Uni<Response> _zinterstore(K destination, K... keys) {
        return _zinterstore(destination, DEFAULT_INSTANCE_AGG, keys);
    }

    Uni<Response> _zlexcount(K key, Range<String> range) {
        nonNull(key, "key");
        nonNull(range, "range");

        return execute(RedisCommand.of(Command.ZLEXCOUNT).put(marshaller.encode(key))
                .put(range.getLowerBound())
                .put(range.getUpperBound()));
    }

    Uni<Response> _zmpopMin(K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        RedisCommand cmd = RedisCommand.of(Command.ZMPOP).put(keys.length).putAll(marshaller.encode(keys)).put("MIN");
        return execute(cmd);
    }

    Uni<Response> _zmpopMin(int count, K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        positive(count, "count");
        RedisCommand cmd = RedisCommand.of(Command.ZMPOP).put(keys.length).putAll(marshaller.encode(keys)).put("MIN")
                .put("COUNT").put(count);
        return execute(cmd);
    }

    Uni<Response> _zmpopMax(K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        RedisCommand cmd = RedisCommand.of(Command.ZMPOP).put(keys.length).putAll(marshaller.encode(keys)).put("MAX");
        return execute(cmd);
    }

    Uni<Response> _zmpopMax(int count, K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        positive(count, "count");
        RedisCommand cmd = RedisCommand.of(Command.ZMPOP).put(keys.length).putAll(marshaller.encode(keys)).put("MAX")
                .put("COUNT").put(count);
        return execute(cmd);
    }

    Uni<Response> _bzmpopMin(Duration timeout, K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        validate(timeout, "timeout");

        RedisCommand cmd = RedisCommand.of(Command.BZMPOP).put(timeout.toSeconds())
                .put(keys.length).putAll(marshaller.encode(keys)).put("MIN");
        return execute(cmd);
    }

    Uni<Response> _bzmpopMin(Duration timeout, int count, K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        validate(timeout, "timeout");

        RedisCommand cmd = RedisCommand.of(Command.BZMPOP).put(timeout.toSeconds())
                .put(keys.length).putAll(marshaller.encode(keys)).put("MIN").put("COUNT").put(count);
        return execute(cmd);
    }

    Uni<Response> _bzmpopMax(Duration timeout, K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        validate(timeout, "timeout");

        RedisCommand cmd = RedisCommand.of(Command.BZMPOP).put(timeout.toSeconds())
                .put(keys.length).putAll(marshaller.encode(keys)).put("MAX");
        return execute(cmd);
    }

    Uni<Response> _bzmpopMax(Duration timeout, int count, K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        validate(timeout, "timeout");

        RedisCommand cmd = RedisCommand.of(Command.BZMPOP).put(timeout.toSeconds())
                .put(keys.length).putAll(marshaller.encode(keys)).put("MAX").put("COUNT").put(count);
        return execute(cmd);
    }

    Uni<Response> _zmscore(K key, V... values) {
        nonNull(key, "key");
        notNullOrEmpty(values, "values");
        RedisCommand cmd = RedisCommand.of(Command.ZMSCORE)
                .put(marshaller.encode(key))
                .putAll(marshaller.encode(values));
        return execute(cmd);
    }

    Uni<Response> _zpopmax(K key) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.ZPOPMAX).put(marshaller.encode(key)));
    }

    Uni<Response> _zpopmax(K key, int count) {
        nonNull(key, "key");
        ParameterValidation.positive(count, "count");
        RedisCommand cmd = RedisCommand.of(Command.ZPOPMAX).put(marshaller.encode(key)).put(count);
        return execute(cmd);
    }

    Uni<Response> _zpopmin(K key) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.ZPOPMIN).put(marshaller.encode(key)));
    }

    Uni<Response> _zpopmin(K key, int count) {
        nonNull(key, "key");
        ParameterValidation.positive(count, "count");
        RedisCommand cmd = RedisCommand.of(Command.ZPOPMIN).put(marshaller.encode(key)).put(count);
        return execute(cmd);
    }

    Uni<Response> _zrandmember(K key) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.ZRANDMEMBER).put(marshaller.encode(key)));
    }

    Uni<Response> _zrandmember(K key, int count) {
        nonNull(key, "key");
        ParameterValidation.positive(count, "count");
        return execute(RedisCommand.of(Command.ZRANDMEMBER).put(marshaller.encode(key)).put(count));
    }

    Uni<Response> _zrandmemberWithScores(K key) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.ZRANDMEMBER).put(marshaller.encode(key)).put(1).put("WITHSCORES"));
    }

    Uni<Response> _zrandmemberWithScores(K key, int count) {
        nonNull(key, "key");
        ParameterValidation.positive(count, "count");
        return execute(RedisCommand.of(Command.ZRANDMEMBER).put(marshaller.encode(key)).put(count).put("WITHSCORES"));
    }

    @SafeVarargs
    final Uni<Response> _bzpopmin(Duration timeout, K... keys) {
        nonNull(keys, "keys");
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        validate(timeout, "timeout");
        RedisCommand cmd = RedisCommand.of(Command.BZPOPMIN);
        cmd.putAll(marshaller.encode(keys)).put(timeout.toSeconds());
        return execute(cmd);
    }

    @SafeVarargs
    final Uni<Response> _bzpopmax(Duration timeout, K... keys) {
        nonNull(keys, "keys");
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        validate(timeout, "timeout");
        RedisCommand cmd = RedisCommand.of(Command.BZPOPMAX);
        for (K key : keys) {
            cmd.put(marshaller.encode(key));
        }
        cmd.put(timeout.toSeconds());
        return execute(cmd);
    }

    Uni<Response> _zrange(K key, long start, long stop, ZRangeArgs args) {
        nonNull(key, "key");
        nonNull(args, "args");

        RedisCommand cmd = RedisCommand.of(Command.ZRANGE)
                .put(marshaller.encode(key)).put(start).put(stop).putArgs(args);

        return execute(cmd);
    }

    Uni<Response> _zrangeWithScores(K key, long start, long stop, ZRangeArgs args) {
        nonNull(key, "key");
        nonNull(args, "args");

        RedisCommand cmd = RedisCommand.of(Command.ZRANGE)
                .put(marshaller.encode(key)).put(start).put(stop).putArgs(args).put("WITHSCORES");

        return execute(cmd);
    }

    Uni<Response> _zrange(K key, long start, long stop) {
        return _zrange(key, start, stop, DEFAULT_INSTANCE_RANGE);
    }

    Uni<Response> _zrangeWithScores(K key, long start, long stop) {
        return _zrangeWithScores(key, start, stop, DEFAULT_INSTANCE_RANGE);
    }

    Uni<Response> _zrangebylex(K key, Range<String> range, ZRangeArgs args) {
        nonNull(key, "key");
        nonNull(args, "args");
        nonNull(range, "range");

        RedisCommand cmd = RedisCommand.of(Command.ZRANGE);
        if (args.isReverse()) {
            // Switch bounds
            cmd.put(marshaller.encode(key)).put(range.getUpperBound()).put(range.getLowerBound()).put("BYLEX").putArgs(args);
        } else {
            cmd.put(marshaller.encode(key)).put(range.getLowerBound()).put(range.getUpperBound()).put("BYLEX").putArgs(args);
        }

        return execute(cmd);
    }

    Uni<Response> _zrangebylex(K key, Range<String> range) {
        return _zrangebylex(key, range, DEFAULT_INSTANCE_RANGE);
    }

    Uni<Response> _zrangebyscore(K key, ScoreRange<Double> range, ZRangeArgs args) {
        nonNull(key, "key");
        nonNull(args, "args");
        nonNull(range, "range");
        RedisCommand cmd = RedisCommand.of(Command.ZRANGE);
        if (args.isReverse() && range.isUnbounded()) {
            // Switch bounds
            cmd.put(marshaller.encode(key)).put(range.getUpperBound()).put(range.getLowerBound()).put("BYSCORE").putArgs(args);
        } else {
            cmd.put(marshaller.encode(key)).put(range.getLowerBound()).put(range.getUpperBound()).put("BYSCORE").putArgs(args);
        }

        return execute(cmd);
    }

    Uni<Response> _zrangebyscoreWithScores(K key, ScoreRange<Double> range, ZRangeArgs args) {
        nonNull(key, "key");
        nonNull(args, "args");
        nonNull(range, "range");

        RedisCommand cmd = RedisCommand.of(Command.ZRANGE);
        if (args.isReverse() && range.isUnbounded()) {
            cmd.put(marshaller.encode(key)).put(range.getUpperBound()).put(range.getLowerBound()).put("BYSCORE")
                    .putArgs(args).put("WITHSCORES");
        } else {
            cmd.put(marshaller.encode(key)).put(range.getLowerBound()).put(range.getUpperBound()).put("BYSCORE")
                    .putArgs(args).put("WITHSCORES");
        }

        return execute(cmd);
    }

    Uni<Response> _zrangebyscore(K key, ScoreRange<Double> range) {
        return _zrangebyscore(key, range, DEFAULT_INSTANCE_RANGE);
    }

    Uni<Response> _zrangebyscoreWithScores(K key, ScoreRange<Double> range) {
        return _zrangebyscoreWithScores(key, range, DEFAULT_INSTANCE_RANGE);
    }

    Uni<Response> _zrangestore(K dst, K src, long min, long max, ZRangeArgs args) {
        nonNull(dst, "dst");
        nonNull(src, "src");
        nonNull(args, "args");

        RedisCommand cmd = RedisCommand.of(Command.ZRANGESTORE)
                .put(marshaller.encode(dst)).put(marshaller.encode(src))
                .put(min).put(max)
                .putArgs(args);

        return execute(cmd);

    }

    Uni<Response> _zrangestore(K dst, K src, long min, long max) {
        return _zrangestore(dst, src, min, max, DEFAULT_INSTANCE_RANGE);
    }

    Uni<Response> _zrangestorebylex(K dst, K src, Range<String> range, ZRangeArgs args) {
        nonNull(dst, "dst");
        nonNull(src, "src");
        nonNull(range, "range");
        nonNull(args, "args");

        RedisCommand cmd = RedisCommand.of(Command.ZRANGESTORE)
                .put(marshaller.encode(dst)).put(marshaller.encode(src))
                .put(range.getLowerBound()).put(range.getUpperBound())
                .put("BYLEX")
                .putArgs(args);

        return execute(cmd);
    }

    Uni<Response> _zrangestorebylex(K dst, K src, Range<String> range) {
        return _zrangestorebylex(dst, src, range, DEFAULT_INSTANCE_RANGE);
    }

    Uni<Response> _zrangestorebyscore(K dst, K src, ScoreRange<Double> range, ZRangeArgs args) {
        nonNull(dst, "dst");
        nonNull(src, "src");
        nonNull(range, "range");
        nonNull(args, "args");

        RedisCommand cmd = RedisCommand.of(Command.ZRANGESTORE)
                .put(marshaller.encode(dst)).put(marshaller.encode(src))
                .put(range.getLowerBound()).put(range.getUpperBound())
                .put("BYSCORE")
                .putArgs(args);
        return execute(cmd);
    }

    Uni<Response> _zrangestorebyscore(K dst, K src, ScoreRange<Double> range) {
        return _zrangestorebyscore(dst, src, range, DEFAULT_INSTANCE_RANGE);
    }

    Uni<Response> _zrank(K key, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        return execute(RedisCommand.of(Command.ZRANK).put(marshaller.encode(key)).put(marshaller.encode(value)));
    }

    Uni<Response> _zrem(K key, V... values) {
        nonNull(key, "key");
        notNullOrEmpty(values, "values");
        doesNotContainNull(values, "values");

        RedisCommand cmd = RedisCommand.of(Command.ZREM)
                .put(marshaller.encode(key));
        for (V value : values) {
            cmd.put(marshaller.encode(value));
        }
        return execute(cmd);
    }

    Uni<Response> _zremrangebylex(K key, Range<String> range) {
        nonNull(key, "key");
        nonNull(range, "range");

        return execute(RedisCommand.of(Command.ZREMRANGEBYLEX).put(marshaller.encode(key))
                .put(range.getLowerBound())
                .put(range.getUpperBound()));
    }

    Uni<Response> _zremrangebyrank(K key, long start, long stop) {
        nonNull(key, "key");

        return execute(RedisCommand.of(Command.ZREMRANGEBYRANK).put(marshaller.encode(key))
                .put(start)
                .put(stop));
    }

    Uni<Response> _zremrangebyscore(K key, ScoreRange<Double> range) {
        nonNull(key, "key");
        nonNull(range, "range");

        return execute(RedisCommand.of(Command.ZREMRANGEBYSCORE).put(marshaller.encode(key))
                .put(range.getLowerBound())
                .put(range.getUpperBound()));
    }

    Uni<Response> _zrevrank(K key, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        return execute(RedisCommand.of(Command.ZREVRANK).put(marshaller.encode(key))
                .put(marshaller.encode(value)));
    }

    ReactiveZScanCursor<V> _zscan(K key) {
        nonNull(key, "key");
        return new ZScanReactiveCursorImpl<>(redis, key, marshaller, typeOfValue, Collections.emptyList());
    }

    ReactiveZScanCursor<V> _zscan(K key, ScanArgs args) {
        nonNull(key, "key");
        nonNull(args, "args");
        return new ZScanReactiveCursorImpl<>(redis, key, marshaller, typeOfValue, args.toArgs());
    }

    Uni<Response> _zscore(K key, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        return execute(RedisCommand.of(Command.ZSCORE).put(marshaller.encode(key))
                .put(marshaller.encode(value)));
    }

    Uni<Response> _zunion(ZAggregateArgs args, K... keys) {
        nonNull(args, "args");
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");

        RedisCommand cmd = RedisCommand.of(Command.ZUNION)
                .put(keys.length);
        for (K key : keys) {
            cmd.put(marshaller.encode(key));
        }
        cmd.putArgs(args);
        return execute(cmd);
    }

    Uni<Response> _zunion(K... keys) {
        return _zunion(DEFAULT_INSTANCE_AGG, keys);
    }

    Uni<Response> _zunionWithScores(K... keys) {
        return _zunionWithScores(DEFAULT_INSTANCE_AGG, keys);
    }

    Uni<Response> _zunionWithScores(ZAggregateArgs args, K... keys) {
        nonNull(args, "args");
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");

        RedisCommand cmd = RedisCommand.of(Command.ZUNION)
                .put(keys.length);

        for (K key : keys) {
            cmd.put(marshaller.encode(key));
        }

        cmd.putArgs(args).put("WITHSCORES");
        return execute(cmd);
    }

    Uni<Response> _zunionstore(K destination, ZAggregateArgs args, K... keys) {
        nonNull(destination, "destination");
        nonNull(args, "args");
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");

        RedisCommand cmd = RedisCommand.of(Command.ZUNIONSTORE)
                .put(marshaller.encode(destination))
                .put(keys.length);

        for (K key : keys) {
            cmd.put(marshaller.encode(key));
        }

        cmd.putArgs(args);
        return execute(cmd);
    }

    Uni<Response> _zunionstore(K destination, K... keys) {
        return _zunionstore(destination, DEFAULT_INSTANCE_AGG, keys);
    }

    protected String getScoreAsString(double score) {
        if (score == Double.MIN_VALUE || score == NEGATIVE_INFINITY) {
            return "-inf";
        } else if (score == Double.MAX_VALUE || score == POSITIVE_INFINITY) {
            return "+inf";
        } else {
            return Double.toString(score);
        }
    }

    final List<ScoredValue<V>> decodeAsListOfScoredValues(Response response) {
        List<ScoredValue<V>> list = new ArrayList<>();
        if (response == null || !response.iterator().hasNext()) {
            return Collections.emptyList();
        }
        if (response.iterator().next().type() == ResponseType.BULK) {
            // Redis 5
            V current = null;
            for (Response nested : response) {
                if (current == null) {
                    current = decodeV(nested);
                } else {
                    list.add(ScoredValue.of(current, nested.toDouble()));
                    current = null;
                }
            }
            return list;
        } else {
            for (Response r : response) {
                list.add(decodeAsScoredValue(r));
            }
            return list;
        }

    }

    ScoredValue<V> decodeAsScoredValue(Response r) {
        if (r == null || r.getDelegate() == null) {
            return null;
        }

        if (r.size() == 0) {
            return ScoredValue.empty();
        }
        return ScoredValue.of(decodeV(r.get(0)), r.get(1).toDouble());

    }

    Double decodeAsDouble(Response r) {
        if (r == null) {
            return null;
        }
        return r.toDouble();
    }

    Long decodeAsLong(Response r) {
        if (r == null) {
            return null;
        }
        return r.toLong();
    }

    long decodeLongOrZero(Response r) {
        if (r == null) {
            return 0L;
        }
        return r.toLong();
    }

    List<Double> decodeAsListOfDouble(Response response) {
        return marshaller.decodeAsList(response, nested -> {
            if (nested == null) {
                return null;
            }
            return nested.toDouble();
        });
    }

    List<V> decodeAsListOfValues(Response r) {
        return marshaller.decodeAsList(r, typeOfValue);
    }

    ScoredValue<V> decodeAsScoredValueOrEmpty(Response r) {
        if (r == null || r.size() == 0) {
            return ScoredValue.empty();
        }
        return decodeAsScoredValue(r.get(0));
    }

    List<ScoredValue<V>> decodePopResponseWithCount(Response r) {
        if (r != null && r.getDelegate() != null && r.size() > 1) {
            return decodeAsListOfScoredValues(r.get(1));
        }
        return Collections.emptyList();
    }

    ScoredValue<V> decodePopResponse(Response r) {
        if (r == null || r.getDelegate() == null) {
            return null;
        }
        List<ScoredValue<V>> values = decodeAsListOfScoredValues(r.get(1));
        if (values.size() == 1) {
            return values.get(0);
        }
        return null;
    }

    V decodeV(Response r) {
        return marshaller.decode(typeOfValue, r);
    }

    boolean decodeIntAsBoolean(Response r) {
        return r.toInteger() == 1;
    }

    KeyValue<K, ScoredValue<V>> decodeAsKeyValue(Response r) {
        if (r == null) {
            return null;
        }
        return KeyValue.of(
                marshaller.decode(typeOfKey, r.get(0)),
                ScoredValue.of(decodeV(r.get(1)), r.get(2).toDouble()));
    }

}
