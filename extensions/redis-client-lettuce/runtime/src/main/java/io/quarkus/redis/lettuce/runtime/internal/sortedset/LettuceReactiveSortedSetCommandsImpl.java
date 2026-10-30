package io.quarkus.redis.lettuce.runtime.internal.sortedset;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.quarkus.redis.runtime.datasource.Validation.validateTimeout;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.lettuce.core.Limit;
import io.lettuce.core.ZPopArgs;
import io.lettuce.core.ZRange;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.ScanArgs;
import io.quarkus.redis.datasource.SortArgs;
import io.quarkus.redis.datasource.list.KeyValue;
import io.quarkus.redis.datasource.sortedset.Range;
import io.quarkus.redis.datasource.sortedset.ReactiveSortedSetCommands;
import io.quarkus.redis.datasource.sortedset.ReactiveZScanCursor;
import io.quarkus.redis.datasource.sortedset.ScoreRange;
import io.quarkus.redis.datasource.sortedset.ScoredValue;
import io.quarkus.redis.datasource.sortedset.ZAddArgs;
import io.quarkus.redis.datasource.sortedset.ZAggregateArgs;
import io.quarkus.redis.datasource.sortedset.ZRangeArgs;
import io.quarkus.redis.lettuce.runtime.internal.AbstractLettuceCommands;
import io.quarkus.redis.lettuce.runtime.internal.LettuceCommand;
import io.quarkus.redis.lettuce.runtime.internal.LettuceCommonConverters;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnection;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnectionPool;
import io.quarkus.redis.runtime.datasource.Marshaller;
import io.smallrye.mutiny.Uni;

public class LettuceReactiveSortedSetCommandsImpl<K, V> extends AbstractLettuceCommands<K, V>
        implements ReactiveSortedSetCommands<K, V> {

    private static final ZAggregateArgs DEFAULT_INSTANCE_AGG = new ZAggregateArgs();
    private static final ZRangeArgs DEFAULT_INSTANCE_RANGE = new ZRangeArgs();

    private final ReactiveRedisDataSource dataSource;

    public LettuceReactiveSortedSetCommandsImpl(ReactiveRedisDataSource dataSource,
            LettuceConnection connection, LettuceConnectionPool pool, Type keyType,
            Type valueType) {
        super(connection, keyType, valueType, new Marshaller(keyType, valueType), pool);
        this.dataSource = dataSource;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Uni<Boolean> zadd(K key, double score, V value) {
        return zadd(key, new ZAddArgs(), score, value);
    }

    @Override
    public Uni<Integer> zadd(K key, Map<V, Double> items) {
        return zadd(key, new ZAddArgs(), items);
    }

    @SafeVarargs
    @Override
    public final Uni<Integer> zadd(K key, ScoredValue<V>... items) {
        return zadd(key, new ZAddArgs(), items);
    }

    @Override
    public Uni<Boolean> zadd(K key, ZAddArgs args, double score, V value) {
        return _zadd(key, args, score, value).toUni();
    }

    LettuceCommand<Long, Boolean> _zadd(K key, ZAddArgs args, double score, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        nonNull(args, "args");
        io.lettuce.core.ZAddArgs lettuceArgs = LettuceSortedSetCommandsConverters.toLettuceZAddArgs(args);
        double normalized = normalizeScore(score);
        return LettuceCommand.of(
                () -> async.zadd(marshaller.encode(key), lettuceArgs, normalized, marshaller.encode(value)),
                AbstractLettuceCommands::toBoolean);
    }

    LettuceCommand<Long, Boolean> _zadd(K key, double score, V value) {
        return _zadd(key, new ZAddArgs(), score, value);
    }

    @Override
    public Uni<Integer> zadd(K key, ZAddArgs args, Map<V, Double> items) {
        return _zadd(key, args, items).toUni();
    }

    LettuceCommand<Long, Integer> _zadd(K key, ZAddArgs args, Map<V, Double> items) {
        nonNull(key, "key");
        nonNull(items, "items");
        nonNull(args, "args");

        io.lettuce.core.ZAddArgs lettuceArgs = LettuceSortedSetCommandsConverters.toLettuceZAddArgs(args);
        @SuppressWarnings("unchecked")
        io.lettuce.core.ScoredValue<byte[]>[] array = items.entrySet().stream()
                .map(entry -> io.lettuce.core.ScoredValue.just(
                        normalizeScore(nonNull(entry.getValue(), "value from items")),
                        marshaller.encode(entry.getKey())))
                .toArray(io.lettuce.core.ScoredValue[]::new);
        return LettuceCommand.of(() -> async.zadd(marshaller.encode(key), lettuceArgs, array),
                AbstractLettuceCommands::toInteger);
    }

    LettuceCommand<Long, Integer> _zadd(K key, Map<V, Double> items) {
        return _zadd(key, new ZAddArgs(), items);
    }

    @SafeVarargs
    @Override
    public final Uni<Integer> zadd(K key, ZAddArgs args, ScoredValue<V>... items) {
        return _zadd(key, args, items).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Long, Integer> _zadd(K key, ZAddArgs args, ScoredValue<V>... items) {
        nonNull(key, "key");
        nonNull(items, "items");
        nonNull(args, "args");

        io.lettuce.core.ZAddArgs lettuceArgs = LettuceSortedSetCommandsConverters.toLettuceZAddArgs(args);
        @SuppressWarnings("unchecked")
        io.lettuce.core.ScoredValue<byte[]>[] array = Arrays.stream(items)
                .map(item -> io.lettuce.core.ScoredValue.just(normalizeScore(item.score),
                        marshaller.encode(nonNull(item.value, "value from scored value"))))
                .toArray(io.lettuce.core.ScoredValue[]::new);
        return LettuceCommand.of(() -> async.zadd(marshaller.encode(key), lettuceArgs, array),
                AbstractLettuceCommands::toInteger);
    }

    @SafeVarargs
    final LettuceCommand<Long, Integer> _zadd(K key, ScoredValue<V>... items) {
        return _zadd(key, new ZAddArgs(), items);
    }

    @Override
    public Uni<Double> zaddincr(K key, double score, V value) {
        return zaddincr(key, new ZAddArgs(), score, value);
    }

    @Override
    public Uni<Double> zaddincr(K key, ZAddArgs args, double score, V value) {
        return _zaddincr(key, args, score, value).toUni();
    }

    LettuceCommand<Double, Double> _zaddincr(K key, ZAddArgs args, double score, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        nonNull(args, "args");
        io.lettuce.core.ZAddArgs lettuceArgs = LettuceSortedSetCommandsConverters.toLettuceZAddArgs(args);
        double normalized = normalizeScore(score);
        return LettuceCommand.of(
                () -> async.zaddincr(marshaller.encode(key), lettuceArgs, normalized, marshaller.encode(value)));
    }

    LettuceCommand<Double, Double> _zaddincr(K key, double score, V value) {
        return _zaddincr(key, new ZAddArgs(), score, value);
    }

    @Override
    public Uni<Long> zcard(K key) {
        return _zcard(key).toUni();
    }

    LettuceCommand<Long, Long> _zcard(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.zcard(marshaller.encode(key)), AbstractLettuceCommands::orZero);
    }

    @Override
    public Uni<Long> zcount(K key, ScoreRange<Double> range) {
        return _zcount(key, range).toUni();
    }

    LettuceCommand<Long, Long> _zcount(K key, ScoreRange<Double> range) {
        nonNull(key, "key");
        nonNull(range, "range");
        io.lettuce.core.Range<Number> lettuceRange = LettuceSortedSetCommandsConverters.toLettuceScoreRange(range);
        return LettuceCommand.of(() -> async.zcount(marshaller.encode(key), lettuceRange));
    }

    @SafeVarargs
    @Override
    public final Uni<List<V>> zdiff(K... keys) {
        return _zdiff(keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<List<byte[]>, List<V>> _zdiff(K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length < 2) {
            return LettuceCommand.failing(new IllegalArgumentException("`keys` must contain at least 2 keys"));
        }
        return LettuceCommand.of(() -> async.zdiff(marshaller.encodeAsArray(keys)), this::decodeListOfValues);
    }

    @SafeVarargs
    @Override
    public final Uni<List<ScoredValue<V>>> zdiffWithScores(K... keys) {
        return _zdiffWithScores(keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<List<io.lettuce.core.ScoredValue<byte[]>>, List<ScoredValue<V>>> _zdiffWithScores(K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length < 2) {
            return LettuceCommand.failing(new IllegalArgumentException("`keys` must contain at least 2 keys"));
        }
        return LettuceCommand.of(() -> async.zdiffWithScores(marshaller.encodeAsArray(keys)), this::decodeScoredValues);
    }

    @SafeVarargs
    @Override
    public final Uni<Long> zdiffstore(K destination, K... keys) {
        return _zdiffstore(destination, keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Long, Long> _zdiffstore(K destination, K... keys) {
        nonNull(destination, "destination");
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length < 2) {
            return LettuceCommand.failing(new IllegalArgumentException("`keys` must contain at least 2 keys"));
        }
        return LettuceCommand.of(() -> async.zdiffstore(marshaller.encode(destination), marshaller.encodeAsArray(keys)));
    }

    @Override
    public Uni<Double> zincrby(K key, double increment, V value) {
        return _zincrby(key, increment, value).toUni();
    }

    LettuceCommand<Double, Double> _zincrby(K key, double increment, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        double normalized = normalizeScore(increment);
        return LettuceCommand.of(() -> async.zincrby(marshaller.encode(key), normalized, marshaller.encode(value)));
    }

    @SafeVarargs
    @Override
    public final Uni<List<V>> zinter(ZAggregateArgs args, K... keys) {
        return _zinter(args, keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<List<byte[]>, List<V>> _zinter(ZAggregateArgs args, K... keys) {
        nonNull(args, "args");
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length < 2) {
            return LettuceCommand.failing(new IllegalArgumentException("`keys` must contain at least 2 keys"));
        }
        io.lettuce.core.ZAggregateArgs lettuceArgs = LettuceSortedSetCommandsConverters.toLettuceZAggregateArgs(args);
        return LettuceCommand.of(() -> async.zinter(lettuceArgs, marshaller.encodeAsArray(keys)),
                this::decodeListOfValues);
    }

    @SafeVarargs
    @Override
    public final Uni<List<V>> zinter(K... keys) {
        return zinter(DEFAULT_INSTANCE_AGG, keys);
    }

    @SafeVarargs
    final LettuceCommand<List<byte[]>, List<V>> _zinter(K... keys) {
        return _zinter(DEFAULT_INSTANCE_AGG, keys);
    }

    @SafeVarargs
    @Override
    public final Uni<List<ScoredValue<V>>> zinterWithScores(ZAggregateArgs arguments, K... keys) {
        return _zinterWithScores(arguments, keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<List<io.lettuce.core.ScoredValue<byte[]>>, List<ScoredValue<V>>> _zinterWithScores(
            ZAggregateArgs arguments, K... keys) {
        nonNull(arguments, "arguments");
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length < 2) {
            return LettuceCommand.failing(new IllegalArgumentException("`keys` must contain at least 2 keys"));
        }
        io.lettuce.core.ZAggregateArgs lettuceArgs = LettuceSortedSetCommandsConverters
                .toLettuceZAggregateArgs(arguments);
        return LettuceCommand.of(() -> async.zinterWithScores(lettuceArgs, marshaller.encodeAsArray(keys)),
                this::decodeScoredValues);
    }

    @SafeVarargs
    @Override
    public final Uni<List<ScoredValue<V>>> zinterWithScores(K... keys) {
        return zinterWithScores(DEFAULT_INSTANCE_AGG, keys);
    }

    @SafeVarargs
    final LettuceCommand<List<io.lettuce.core.ScoredValue<byte[]>>, List<ScoredValue<V>>> _zinterWithScores(K... keys) {
        return _zinterWithScores(DEFAULT_INSTANCE_AGG, keys);
    }

    @SafeVarargs
    @Override
    public final Uni<Long> zintercard(K... keys) {
        return _zintercard(keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Long, Long> _zintercard(K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length < 2) {
            return LettuceCommand.failing(new IllegalArgumentException("`keys` must contain at least 2 keys"));
        }
        return LettuceCommand.of(() -> async.zintercard(marshaller.encodeAsArray(keys)));
    }

    @SafeVarargs
    @Override
    public final Uni<Long> zintercard(long limit, K... keys) {
        return _zintercard(limit, keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Long, Long> _zintercard(long limit, K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length < 2) {
            return LettuceCommand.failing(new IllegalArgumentException("`keys` must contain at least 2 keys"));
        }
        positive(limit, "limit");
        return LettuceCommand.of(() -> async.zintercard(limit, marshaller.encodeAsArray(keys)));
    }

    @SafeVarargs
    @Override
    public final Uni<Long> zinterstore(K destination, ZAggregateArgs arguments, K... keys) {
        return _zinterstore(destination, arguments, keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Long, Long> _zinterstore(K destination, ZAggregateArgs arguments, K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length < 2) {
            return LettuceCommand.failing(new IllegalArgumentException("`keys` must contain at least 2 keys"));
        }
        nonNull(arguments, "arguments");
        nonNull(destination, "destination");
        io.lettuce.core.ZStoreArgs lettuceArgs = LettuceSortedSetCommandsConverters.toLettuceZStoreArgs(arguments);
        return LettuceCommand.of(
                () -> async.zinterstore(marshaller.encode(destination), lettuceArgs, marshaller.encodeAsArray(keys)));
    }

    @SafeVarargs
    @Override
    public final Uni<Long> zinterstore(K destination, K... keys) {
        return zinterstore(destination, DEFAULT_INSTANCE_AGG, keys);
    }

    @SafeVarargs
    final LettuceCommand<Long, Long> _zinterstore(K destination, K... keys) {
        return _zinterstore(destination, DEFAULT_INSTANCE_AGG, keys);
    }

    @Override
    public Uni<Long> zlexcount(K key, Range<String> range) {
        return _zlexcount(key, range).toUni();
    }

    LettuceCommand<Long, Long> _zlexcount(K key, Range<String> range) {
        nonNull(key, "key");
        nonNull(range, "range");
        io.lettuce.core.Range<byte[]> lettuceRange = LettuceSortedSetCommandsConverters.toLettuceLexRange(range);
        return LettuceCommand.of(() -> async.zlexcount(marshaller.encode(key), lettuceRange));
    }

    @SafeVarargs
    @Override
    public final Uni<ScoredValue<V>> zmpopMin(K... keys) {
        return _zmpopMin(keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<io.lettuce.core.KeyValue<byte[], io.lettuce.core.ScoredValue<byte[]>>, ScoredValue<V>> _zmpopMin(
            K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        return LettuceCommand.of(() -> async.zmpop(ZPopArgs.Builder.min(), marshaller.encodeAsArray(keys)),
                this::decodePopped);
    }

    @SafeVarargs
    @Override
    public final Uni<List<ScoredValue<V>>> zmpopMin(int count, K... keys) {
        return _zmpopMin(count, keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<io.lettuce.core.KeyValue<byte[], List<io.lettuce.core.ScoredValue<byte[]>>>, List<ScoredValue<V>>> _zmpopMin(
            int count, K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        positive(count, "count");
        return LettuceCommand.of(() -> async.zmpop(count, ZPopArgs.Builder.min(), marshaller.encodeAsArray(keys)),
                this::decodePoppedList);
    }

    @SafeVarargs
    @Override
    public final Uni<ScoredValue<V>> zmpopMax(K... keys) {
        return _zmpopMax(keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<io.lettuce.core.KeyValue<byte[], io.lettuce.core.ScoredValue<byte[]>>, ScoredValue<V>> _zmpopMax(
            K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        return LettuceCommand.of(() -> async.zmpop(ZPopArgs.Builder.max(), marshaller.encodeAsArray(keys)),
                this::decodePopped);
    }

    @SafeVarargs
    @Override
    public final Uni<List<ScoredValue<V>>> zmpopMax(int count, K... keys) {
        return _zmpopMax(count, keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<io.lettuce.core.KeyValue<byte[], List<io.lettuce.core.ScoredValue<byte[]>>>, List<ScoredValue<V>>> _zmpopMax(
            int count, K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        positive(count, "count");
        return LettuceCommand.of(() -> async.zmpop(count, ZPopArgs.Builder.max(), marshaller.encodeAsArray(keys)),
                this::decodePoppedList);
    }

    @SafeVarargs
    @Override
    public final Uni<ScoredValue<V>> bzmpopMin(Duration timeout, K... keys) {
        return blocking(cmds -> _bzmpopMin(cmds, timeout, keys));
    }

    @SafeVarargs
    final LettuceCommand<io.lettuce.core.KeyValue<byte[], io.lettuce.core.ScoredValue<byte[]>>, ScoredValue<V>> _bzmpopMin(
            Duration timeout, K... keys) {
        return _bzmpopMin(async, timeout, keys);
    }

    @SafeVarargs
    final LettuceCommand<io.lettuce.core.KeyValue<byte[], io.lettuce.core.ScoredValue<byte[]>>, ScoredValue<V>> _bzmpopMin(
            RedisClusterAsyncCommands<byte[], byte[]> cmds, Duration timeout, K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        validateTimeout(timeout, "timeout");
        return LettuceCommand.of(() -> {
            if (isWholeSeconds(timeout)) {
                return cmds.bzmpop(timeout.getSeconds(), ZPopArgs.Builder.min(), marshaller.encodeAsArray(keys));
            }
            return cmds.bzmpop(toFractionalSeconds(timeout), ZPopArgs.Builder.min(), marshaller.encodeAsArray(keys));
        }, this::decodePopped);
    }

    @SafeVarargs
    @Override
    public final Uni<List<ScoredValue<V>>> bzmpopMin(Duration timeout, int count, K... keys) {
        return blocking(cmds -> _bzmpopMin(cmds, timeout, count, keys));
    }

    @SafeVarargs
    final LettuceCommand<io.lettuce.core.KeyValue<byte[], List<io.lettuce.core.ScoredValue<byte[]>>>, List<ScoredValue<V>>> _bzmpopMin(
            Duration timeout, int count, K... keys) {
        return _bzmpopMin(async, timeout, count, keys);
    }

    @SafeVarargs
    final LettuceCommand<io.lettuce.core.KeyValue<byte[], List<io.lettuce.core.ScoredValue<byte[]>>>, List<ScoredValue<V>>> _bzmpopMin(
            RedisClusterAsyncCommands<byte[], byte[]> cmds, Duration timeout, int count, K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        validateTimeout(timeout, "timeout");
        positive(count, "count");
        return LettuceCommand.of(() -> {
            if (isWholeSeconds(timeout)) {
                return cmds.bzmpop(timeout.getSeconds(), (long) count, ZPopArgs.Builder.min(),
                        marshaller.encodeAsArray(keys));
            }
            return cmds.bzmpop(toFractionalSeconds(timeout), count, ZPopArgs.Builder.min(),
                    marshaller.encodeAsArray(keys));
        }, this::decodePoppedList);
    }

    @SafeVarargs
    @Override
    public final Uni<ScoredValue<V>> bzmpopMax(Duration timeout, K... keys) {
        return blocking(cmds -> _bzmpopMax(cmds, timeout, keys));
    }

    @SafeVarargs
    final LettuceCommand<io.lettuce.core.KeyValue<byte[], io.lettuce.core.ScoredValue<byte[]>>, ScoredValue<V>> _bzmpopMax(
            Duration timeout, K... keys) {
        return _bzmpopMax(async, timeout, keys);
    }

    @SafeVarargs
    final LettuceCommand<io.lettuce.core.KeyValue<byte[], io.lettuce.core.ScoredValue<byte[]>>, ScoredValue<V>> _bzmpopMax(
            RedisClusterAsyncCommands<byte[], byte[]> cmds, Duration timeout, K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        validateTimeout(timeout, "timeout");
        return LettuceCommand.of(() -> {
            if (isWholeSeconds(timeout)) {
                return cmds.bzmpop(timeout.getSeconds(), ZPopArgs.Builder.max(), marshaller.encodeAsArray(keys));
            }
            return cmds.bzmpop(toFractionalSeconds(timeout), ZPopArgs.Builder.max(), marshaller.encodeAsArray(keys));
        }, this::decodePopped);
    }

    @SafeVarargs
    @Override
    public final Uni<List<ScoredValue<V>>> bzmpopMax(Duration timeout, int count, K... keys) {
        return blocking(cmds -> _bzmpopMax(cmds, timeout, count, keys));
    }

    @SafeVarargs
    final LettuceCommand<io.lettuce.core.KeyValue<byte[], List<io.lettuce.core.ScoredValue<byte[]>>>, List<ScoredValue<V>>> _bzmpopMax(
            Duration timeout, int count, K... keys) {
        return _bzmpopMax(async, timeout, count, keys);
    }

    @SafeVarargs
    final LettuceCommand<io.lettuce.core.KeyValue<byte[], List<io.lettuce.core.ScoredValue<byte[]>>>, List<ScoredValue<V>>> _bzmpopMax(
            RedisClusterAsyncCommands<byte[], byte[]> cmds, Duration timeout, int count, K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        validateTimeout(timeout, "timeout");
        positive(count, "count");
        return LettuceCommand.of(() -> {
            if (isWholeSeconds(timeout)) {
                return cmds.bzmpop(timeout.getSeconds(), (long) count, ZPopArgs.Builder.max(),
                        marshaller.encodeAsArray(keys));
            }
            return cmds.bzmpop(toFractionalSeconds(timeout), count, ZPopArgs.Builder.max(),
                    marshaller.encodeAsArray(keys));
        }, this::decodePoppedList);
    }

    @SafeVarargs
    @Override
    public final Uni<List<Double>> zmscore(K key, V... values) {
        return _zmscore(key, values).toUni();
    }

    @SafeVarargs
    final LettuceCommand<List<Double>, List<Double>> _zmscore(K key, V... values) {
        nonNull(key, "key");
        notNullOrEmpty(values, "values");
        doesNotContainNull(values, "values");
        return LettuceCommand.of(() -> async.zmscore(marshaller.encode(key), marshaller.encodeAsArray(values)),
                AbstractLettuceCommands::decodeList);
    }

    @Override
    public Uni<ScoredValue<V>> zpopmax(K key) {
        return _zpopmax(key).toUni();
    }

    LettuceCommand<io.lettuce.core.ScoredValue<byte[]>, ScoredValue<V>> _zpopmax(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.zpopmax(marshaller.encode(key)), this::decodeScoredValueOrEmpty);
    }

    @Override
    public Uni<List<ScoredValue<V>>> zpopmax(K key, int count) {
        return _zpopmax(key, count).toUni();
    }

    LettuceCommand<List<io.lettuce.core.ScoredValue<byte[]>>, List<ScoredValue<V>>> _zpopmax(K key, int count) {
        nonNull(key, "key");
        positive(count, "count");
        return LettuceCommand.of(() -> async.zpopmax(marshaller.encode(key), count), this::decodeScoredValues);
    }

    @Override
    public Uni<ScoredValue<V>> zpopmin(K key) {
        return _zpopmin(key).toUni();
    }

    LettuceCommand<io.lettuce.core.ScoredValue<byte[]>, ScoredValue<V>> _zpopmin(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.zpopmin(marshaller.encode(key)), this::decodeScoredValueOrEmpty);
    }

    @Override
    public Uni<List<ScoredValue<V>>> zpopmin(K key, int count) {
        return _zpopmin(key, count).toUni();
    }

    LettuceCommand<List<io.lettuce.core.ScoredValue<byte[]>>, List<ScoredValue<V>>> _zpopmin(K key, int count) {
        nonNull(key, "key");
        positive(count, "count");
        return LettuceCommand.of(() -> async.zpopmin(marshaller.encode(key), count), this::decodeScoredValues);
    }

    @Override
    public Uni<V> zrandmember(K key) {
        return _zrandmember(key).toUni();
    }

    LettuceCommand<byte[], V> _zrandmember(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.zrandmember(marshaller.encode(key)), this::decodeValue);
    }

    @Override
    public Uni<List<V>> zrandmember(K key, int count) {
        return _zrandmember(key, count).toUni();
    }

    LettuceCommand<List<byte[]>, List<V>> _zrandmember(K key, int count) {
        nonNull(key, "key");
        positive(count, "count");
        return LettuceCommand.of(() -> async.zrandmember(marshaller.encode(key), count), this::decodeListOfValues);
    }

    @Override
    public Uni<ScoredValue<V>> zrandmemberWithScores(K key) {
        return _zrandmemberWithScores(key).toUni();
    }

    LettuceCommand<io.lettuce.core.ScoredValue<byte[]>, ScoredValue<V>> _zrandmemberWithScores(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.zrandmemberWithScores(marshaller.encode(key)),
                this::decodeScoredValueOrEmpty);
    }

    @Override
    public Uni<List<ScoredValue<V>>> zrandmemberWithScores(K key, int count) {
        return _zrandmemberWithScores(key, count).toUni();
    }

    LettuceCommand<List<io.lettuce.core.ScoredValue<byte[]>>, List<ScoredValue<V>>> _zrandmemberWithScores(K key,
            int count) {
        nonNull(key, "key");
        positive(count, "count");
        return LettuceCommand.of(() -> async.zrandmemberWithScores(marshaller.encode(key), count),
                this::decodeScoredValues);
    }

    @SafeVarargs
    @Override
    public final Uni<KeyValue<K, ScoredValue<V>>> bzpopmin(Duration timeout, K... keys) {
        return blocking(cmds -> _bzpopmin(cmds, timeout, keys));
    }

    @SafeVarargs
    final LettuceCommand<io.lettuce.core.KeyValue<byte[], io.lettuce.core.ScoredValue<byte[]>>, KeyValue<K, ScoredValue<V>>> _bzpopmin(
            Duration timeout, K... keys) {
        return _bzpopmin(async, timeout, keys);
    }

    @SafeVarargs
    final LettuceCommand<io.lettuce.core.KeyValue<byte[], io.lettuce.core.ScoredValue<byte[]>>, KeyValue<K, ScoredValue<V>>> _bzpopmin(
            RedisClusterAsyncCommands<byte[], byte[]> cmds, Duration timeout, K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        validateTimeout(timeout, "timeout");
        return LettuceCommand.of(() -> {
            if (isWholeSeconds(timeout)) {
                return cmds.bzpopmin(timeout.getSeconds(), marshaller.encodeAsArray(keys));
            }
            return cmds.bzpopmin(toFractionalSeconds(timeout), marshaller.encodeAsArray(keys));
        }, this::decodeKeyValue);
    }

    @SafeVarargs
    @Override
    public final Uni<KeyValue<K, ScoredValue<V>>> bzpopmax(Duration timeout, K... keys) {
        return blocking(cmds -> _bzpopmax(cmds, timeout, keys));
    }

    @SafeVarargs
    final LettuceCommand<io.lettuce.core.KeyValue<byte[], io.lettuce.core.ScoredValue<byte[]>>, KeyValue<K, ScoredValue<V>>> _bzpopmax(
            Duration timeout, K... keys) {
        return _bzpopmax(async, timeout, keys);
    }

    @SafeVarargs
    final LettuceCommand<io.lettuce.core.KeyValue<byte[], io.lettuce.core.ScoredValue<byte[]>>, KeyValue<K, ScoredValue<V>>> _bzpopmax(
            RedisClusterAsyncCommands<byte[], byte[]> cmds, Duration timeout, K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        validateTimeout(timeout, "timeout");
        return LettuceCommand.of(() -> {
            if (isWholeSeconds(timeout)) {
                return cmds.bzpopmax(timeout.getSeconds(), marshaller.encodeAsArray(keys));
            }
            return cmds.bzpopmax(toFractionalSeconds(timeout), marshaller.encodeAsArray(keys));
        }, this::decodeKeyValue);
    }

    @Override
    public Uni<List<V>> zrange(K key, long start, long stop, ZRangeArgs args) {
        return _zrange(key, start, stop, args).toUni();
    }

    LettuceCommand<List<byte[]>, List<V>> _zrange(K key, long start, long stop, ZRangeArgs args) {
        nonNull(key, "key");
        nonNull(args, "args");
        ZRange.ByIndex range = LettuceSortedSetCommandsConverters.toLettuceByIndex(start, stop, args);
        return LettuceCommand.of(() -> async.zrange(marshaller.encode(key), range), this::decodeListOfValues);
    }

    LettuceCommand<List<byte[]>, List<V>> _zrange(K key, long start, long stop) {
        return _zrange(key, start, stop, DEFAULT_INSTANCE_RANGE);
    }

    @Override
    public Uni<List<ScoredValue<V>>> zrangeWithScores(K key, long start, long stop, ZRangeArgs args) {
        return _zrangeWithScores(key, start, stop, args).toUni();
    }

    LettuceCommand<List<io.lettuce.core.ScoredValue<byte[]>>, List<ScoredValue<V>>> _zrangeWithScores(K key, long start,
            long stop, ZRangeArgs args) {
        nonNull(key, "key");
        nonNull(args, "args");
        ZRange.ByIndex range = LettuceSortedSetCommandsConverters.toLettuceByIndex(start, stop, args);
        return LettuceCommand.of(() -> async.zrangeWithScores(marshaller.encode(key), range), this::decodeScoredValues);
    }

    LettuceCommand<List<io.lettuce.core.ScoredValue<byte[]>>, List<ScoredValue<V>>> _zrangeWithScores(K key, long start,
            long stop) {
        return _zrangeWithScores(key, start, stop, DEFAULT_INSTANCE_RANGE);
    }

    @Override
    public Uni<List<V>> zrange(K key, long start, long stop) {
        return zrange(key, start, stop, DEFAULT_INSTANCE_RANGE);
    }

    @Override
    public Uni<List<ScoredValue<V>>> zrangeWithScores(K key, long start, long stop) {
        return zrangeWithScores(key, start, stop, DEFAULT_INSTANCE_RANGE);
    }

    @Override
    public Uni<List<V>> zrangebylex(K key, Range<String> range, ZRangeArgs args) {
        return _zrangebylex(key, range, args).toUni();
    }

    LettuceCommand<List<byte[]>, List<V>> _zrangebylex(K key, Range<String> range, ZRangeArgs args) {
        nonNull(key, "key");
        nonNull(args, "args");
        nonNull(range, "range");
        ZRange.ByLex<byte[]> byLex = LettuceSortedSetCommandsConverters.toLettuceByLex(range, args);
        return LettuceCommand.of(() -> async.zrange(marshaller.encode(key), byLex), this::decodeListOfValues);
    }

    LettuceCommand<List<byte[]>, List<V>> _zrangebylex(K key, Range<String> range) {
        return _zrangebylex(key, range, DEFAULT_INSTANCE_RANGE);
    }

    @Override
    public Uni<List<V>> zrangebylex(K key, Range<String> range) {
        return zrangebylex(key, range, DEFAULT_INSTANCE_RANGE);
    }

    @Override
    public Uni<List<V>> zrangebyscore(K key, ScoreRange<Double> range, ZRangeArgs args) {
        return _zrangebyscore(key, range, args).toUni();
    }

    LettuceCommand<List<byte[]>, List<V>> _zrangebyscore(K key, ScoreRange<Double> range, ZRangeArgs args) {
        nonNull(key, "key");
        nonNull(args, "args");
        nonNull(range, "range");
        ZRange.ByScore byScore = LettuceSortedSetCommandsConverters.toLettuceByScore(range, args);
        return LettuceCommand.of(() -> async.zrange(marshaller.encode(key), byScore), this::decodeListOfValues);
    }

    LettuceCommand<List<byte[]>, List<V>> _zrangebyscore(K key, ScoreRange<Double> range) {
        return _zrangebyscore(key, range, DEFAULT_INSTANCE_RANGE);
    }

    @Override
    public Uni<List<ScoredValue<V>>> zrangebyscoreWithScores(K key, ScoreRange<Double> range, ZRangeArgs args) {
        return _zrangebyscoreWithScores(key, range, args).toUni();
    }

    LettuceCommand<List<io.lettuce.core.ScoredValue<byte[]>>, List<ScoredValue<V>>> _zrangebyscoreWithScores(K key,
            ScoreRange<Double> range, ZRangeArgs args) {
        nonNull(key, "key");
        nonNull(args, "args");
        nonNull(range, "range");
        ZRange.ByScore byScore = LettuceSortedSetCommandsConverters.toLettuceByScore(range, args);
        return LettuceCommand.of(() -> async.zrangeWithScores(marshaller.encode(key), byScore), this::decodeScoredValues);
    }

    LettuceCommand<List<io.lettuce.core.ScoredValue<byte[]>>, List<ScoredValue<V>>> _zrangebyscoreWithScores(K key,
            ScoreRange<Double> range) {
        return _zrangebyscoreWithScores(key, range, DEFAULT_INSTANCE_RANGE);
    }

    @Override
    public Uni<List<V>> zrangebyscore(K key, ScoreRange<Double> range) {
        return zrangebyscore(key, range, DEFAULT_INSTANCE_RANGE);
    }

    @Override
    public Uni<List<ScoredValue<V>>> zrangebyscoreWithScores(K key, ScoreRange<Double> range) {
        return zrangebyscoreWithScores(key, range, DEFAULT_INSTANCE_RANGE);
    }

    @Override
    public Uni<Long> zrangestore(K dst, K src, long min, long max, ZRangeArgs args) {
        return _zrangestore(dst, src, min, max, args).toUni();
    }

    LettuceCommand<Long, Long> _zrangestore(K dst, K src, long min, long max, ZRangeArgs args) {
        nonNull(dst, "dst");
        nonNull(src, "src");
        nonNull(args, "args");
        io.lettuce.core.Range<Long> range = LettuceSortedSetCommandsConverters.toLettuceIndexRange(min, max, args);
        if (args.isReverse()) {
            return LettuceCommand.of(() -> async.zrevrangestore(marshaller.encode(dst), marshaller.encode(src), range));
        }
        return LettuceCommand.of(() -> async.zrangestore(marshaller.encode(dst), marshaller.encode(src), range));
    }

    LettuceCommand<Long, Long> _zrangestore(K dst, K src, long min, long max) {
        return _zrangestore(dst, src, min, max, DEFAULT_INSTANCE_RANGE);
    }

    @Override
    public Uni<Long> zrangestore(K dst, K src, long min, long max) {
        return zrangestore(dst, src, min, max, DEFAULT_INSTANCE_RANGE);
    }

    @Override
    public Uni<Long> zrangestorebylex(K dst, K src, Range<String> range, ZRangeArgs args) {
        return _zrangestorebylex(dst, src, range, args).toUni();
    }

    LettuceCommand<Long, Long> _zrangestorebylex(K dst, K src, Range<String> range, ZRangeArgs args) {
        nonNull(dst, "dst");
        nonNull(src, "src");
        nonNull(range, "range");
        nonNull(args, "args");
        io.lettuce.core.Range<byte[]> lettuceRange = LettuceSortedSetCommandsConverters.toLettuceStoreLexRange(range, args);
        Limit limit = LettuceSortedSetCommandsConverters.toLettuceLimit(args);
        if (args.isReverse()) {
            return LettuceCommand.of(
                    () -> async.zrevrangestorebylex(marshaller.encode(dst), marshaller.encode(src), lettuceRange, limit));
        }
        return LettuceCommand.of(
                () -> async.zrangestorebylex(marshaller.encode(dst), marshaller.encode(src), lettuceRange, limit));
    }

    LettuceCommand<Long, Long> _zrangestorebylex(K dst, K src, Range<String> range) {
        return _zrangestorebylex(dst, src, range, DEFAULT_INSTANCE_RANGE);
    }

    @Override
    public Uni<Long> zrangestorebylex(K dst, K src, Range<String> range) {
        return zrangestorebylex(dst, src, range, DEFAULT_INSTANCE_RANGE);
    }

    @Override
    public Uni<Long> zrangestorebyscore(K dst, K src, ScoreRange<Double> range, ZRangeArgs args) {
        return _zrangestorebyscore(dst, src, range, args).toUni();
    }

    LettuceCommand<Long, Long> _zrangestorebyscore(K dst, K src, ScoreRange<Double> range, ZRangeArgs args) {
        nonNull(dst, "dst");
        nonNull(src, "src");
        nonNull(range, "range");
        nonNull(args, "args");
        io.lettuce.core.Range<Number> lettuceRange = LettuceSortedSetCommandsConverters.toLettuceStoreScoreRange(range,
                args);
        Limit limit = LettuceSortedSetCommandsConverters.toLettuceLimit(args);
        if (args.isReverse()) {
            return LettuceCommand.of(
                    () -> async.zrevrangestorebyscore(marshaller.encode(dst), marshaller.encode(src), lettuceRange, limit));
        }
        return LettuceCommand.of(
                () -> async.zrangestorebyscore(marshaller.encode(dst), marshaller.encode(src), lettuceRange, limit));
    }

    LettuceCommand<Long, Long> _zrangestorebyscore(K dst, K src, ScoreRange<Double> range) {
        return _zrangestorebyscore(dst, src, range, DEFAULT_INSTANCE_RANGE);
    }

    @Override
    public Uni<Long> zrangestorebyscore(K dst, K src, ScoreRange<Double> range) {
        return zrangestorebyscore(dst, src, range, DEFAULT_INSTANCE_RANGE);
    }

    @Override
    public Uni<Long> zrank(K key, V value) {
        return _zrank(key, value).toUni();
    }

    LettuceCommand<Long, Long> _zrank(K key, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        return LettuceCommand.of(() -> async.zrank(marshaller.encode(key), marshaller.encode(value)));
    }

    @SafeVarargs
    @Override
    public final Uni<Integer> zrem(K key, V... values) {
        return _zrem(key, values).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Long, Integer> _zrem(K key, V... values) {
        nonNull(key, "key");
        notNullOrEmpty(values, "values");
        doesNotContainNull(values, "values");
        return LettuceCommand.of(() -> async.zrem(marshaller.encode(key), marshaller.encodeAsArray(values)),
                AbstractLettuceCommands::toInteger);
    }

    @Override
    public Uni<Long> zremrangebylex(K key, Range<String> range) {
        return _zremrangebylex(key, range).toUni();
    }

    LettuceCommand<Long, Long> _zremrangebylex(K key, Range<String> range) {
        nonNull(key, "key");
        nonNull(range, "range");
        io.lettuce.core.Range<byte[]> lettuceRange = LettuceSortedSetCommandsConverters.toLettuceLexRange(range);
        return LettuceCommand.of(() -> async.zremrangebylex(marshaller.encode(key), lettuceRange));
    }

    @Override
    public Uni<Long> zremrangebyrank(K key, long start, long stop) {
        return _zremrangebyrank(key, start, stop).toUni();
    }

    LettuceCommand<Long, Long> _zremrangebyrank(K key, long start, long stop) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.zremrangebyrank(marshaller.encode(key), start, stop));
    }

    @Override
    public Uni<Long> zremrangebyscore(K key, ScoreRange<Double> range) {
        return _zremrangebyscore(key, range).toUni();
    }

    LettuceCommand<Long, Long> _zremrangebyscore(K key, ScoreRange<Double> range) {
        nonNull(key, "key");
        nonNull(range, "range");
        io.lettuce.core.Range<Number> lettuceRange = LettuceSortedSetCommandsConverters.toLettuceScoreRange(range);
        return LettuceCommand.of(() -> async.zremrangebyscore(marshaller.encode(key), lettuceRange));
    }

    @Override
    public Uni<Long> zrevrank(K key, V value) {
        return _zrevrank(key, value).toUni();
    }

    LettuceCommand<Long, Long> _zrevrank(K key, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        return LettuceCommand.of(() -> async.zrevrank(marshaller.encode(key), marshaller.encode(value)));
    }

    @Override
    public ReactiveZScanCursor<V> zscan(K key) {
        nonNull(key, "key");
        return new LettuceReactiveZScanCursorImpl<>(async, marshaller.encode(key), this::decodeScoredValues);
    }

    @Override
    public ReactiveZScanCursor<V> zscan(K key, ScanArgs args) {
        nonNull(key, "key");
        nonNull(args, "args");
        return new LettuceReactiveZScanCursorImpl<>(async, marshaller.encode(key),
                LettuceCommonConverters.toLettuceScanArgs(args), this::decodeScoredValues);
    }

    @Override
    public Uni<Double> zscore(K key, V value) {
        return _zscore(key, value).toUni();
    }

    LettuceCommand<Double, Double> _zscore(K key, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        return LettuceCommand.of(() -> async.zscore(marshaller.encode(key), marshaller.encode(value)));
    }

    @SafeVarargs
    @Override
    public final Uni<List<V>> zunion(ZAggregateArgs args, K... keys) {
        return _zunion(args, keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<List<byte[]>, List<V>> _zunion(ZAggregateArgs args, K... keys) {
        nonNull(args, "args");
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        io.lettuce.core.ZAggregateArgs lettuceArgs = LettuceSortedSetCommandsConverters.toLettuceZAggregateArgs(args);
        return LettuceCommand.of(() -> async.zunion(lettuceArgs, marshaller.encodeAsArray(keys)),
                this::decodeListOfValues);
    }

    @SafeVarargs
    @Override
    public final Uni<List<V>> zunion(K... keys) {
        return zunion(DEFAULT_INSTANCE_AGG, keys);
    }

    @SafeVarargs
    final LettuceCommand<List<byte[]>, List<V>> _zunion(K... keys) {
        return _zunion(DEFAULT_INSTANCE_AGG, keys);
    }

    @SafeVarargs
    @Override
    public final Uni<List<ScoredValue<V>>> zunionWithScores(K... keys) {
        return zunionWithScores(DEFAULT_INSTANCE_AGG, keys);
    }

    @SafeVarargs
    final LettuceCommand<List<io.lettuce.core.ScoredValue<byte[]>>, List<ScoredValue<V>>> _zunionWithScores(K... keys) {
        return _zunionWithScores(DEFAULT_INSTANCE_AGG, keys);
    }

    @SafeVarargs
    @Override
    public final Uni<List<ScoredValue<V>>> zunionWithScores(ZAggregateArgs args, K... keys) {
        return _zunionWithScores(args, keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<List<io.lettuce.core.ScoredValue<byte[]>>, List<ScoredValue<V>>> _zunionWithScores(
            ZAggregateArgs args, K... keys) {
        nonNull(args, "args");
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        io.lettuce.core.ZAggregateArgs lettuceArgs = LettuceSortedSetCommandsConverters.toLettuceZAggregateArgs(args);
        return LettuceCommand.of(() -> async.zunionWithScores(lettuceArgs, marshaller.encodeAsArray(keys)),
                this::decodeScoredValues);
    }

    @SafeVarargs
    @Override
    public final Uni<Long> zunionstore(K destination, ZAggregateArgs args, K... keys) {
        return _zunionstore(destination, args, keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Long, Long> _zunionstore(K destination, ZAggregateArgs args, K... keys) {
        nonNull(destination, "destination");
        nonNull(args, "args");
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        io.lettuce.core.ZStoreArgs lettuceArgs = LettuceSortedSetCommandsConverters.toLettuceZStoreArgs(args);
        return LettuceCommand.of(
                () -> async.zunionstore(marshaller.encode(destination), lettuceArgs, marshaller.encodeAsArray(keys)));
    }

    @SafeVarargs
    @Override
    public final Uni<Long> zunionstore(K destination, K... keys) {
        return zunionstore(destination, DEFAULT_INSTANCE_AGG, keys);
    }

    @SafeVarargs
    final LettuceCommand<Long, Long> _zunionstore(K destination, K... keys) {
        return _zunionstore(destination, DEFAULT_INSTANCE_AGG, keys);
    }

    @Override
    public Uni<List<V>> sort(K key) {
        return sort(key, new SortArgs());
    }

    @Override
    public Uni<List<V>> sort(K key, SortArgs sortArguments) {
        return _sort(key, sortArguments).toUni();
    }

    LettuceCommand<List<byte[]>, List<V>> _sort(K key, SortArgs sortArguments) {
        nonNull(key, "key");
        nonNull(sortArguments, "sortArguments");
        io.lettuce.core.SortArgs lettuceArgs = LettuceCommonConverters.toLettuceSortArgs(sortArguments);
        return LettuceCommand.of(() -> async.sort(marshaller.encode(key), lettuceArgs), this::decodeListOfValues);
    }

    @Override
    public Uni<Long> sortAndStore(K key, K destination, SortArgs sortArguments) {
        return _sortAndStore(key, destination, sortArguments).toUni();
    }

    LettuceCommand<Long, Long> _sortAndStore(K key, K destination, SortArgs args) {
        nonNull(key, "key");
        nonNull(destination, "destination");
        nonNull(args, "args");
        io.lettuce.core.SortArgs lettuceArgs = LettuceCommonConverters.toLettuceSortArgs(args);
        return LettuceCommand.of(() -> async.sortStore(marshaller.encode(key), lettuceArgs, marshaller.encode(destination)));
    }

    @Override
    public Uni<Long> sortAndStore(K key, K destination) {
        return sortAndStore(key, destination, new SortArgs());
    }

    private ScoredValue<V> decodeScoredValue(io.lettuce.core.ScoredValue<byte[]> value) {
        if (value == null || !value.hasValue()) {
            return null;
        }
        return ScoredValue.of(decodeValue(value.getValue()), value.getScore());
    }

    private ScoredValue<V> decodeScoredValueOrEmpty(io.lettuce.core.ScoredValue<byte[]> value) {
        ScoredValue<V> converted = decodeScoredValue(value);
        if (converted == null) {
            return ScoredValue.empty();
        }
        return converted;
    }

    private List<ScoredValue<V>> decodeScoredValues(List<io.lettuce.core.ScoredValue<byte[]>> values) {
        return decodeList(values, this::decodeScoredValue);
    }

    private ScoredValue<V> decodePopped(io.lettuce.core.KeyValue<byte[], io.lettuce.core.ScoredValue<byte[]>> result) {
        if (result == null || !result.hasValue()) {
            return null;
        }
        return decodeScoredValue(result.getValue());
    }

    private List<ScoredValue<V>> decodePoppedList(
            io.lettuce.core.KeyValue<byte[], List<io.lettuce.core.ScoredValue<byte[]>>> result) {
        if (result == null || !result.hasValue()) {
            return Collections.emptyList();
        }
        return decodeScoredValues(result.getValue());
    }

    private KeyValue<K, ScoredValue<V>> decodeKeyValue(
            io.lettuce.core.KeyValue<byte[], io.lettuce.core.ScoredValue<byte[]>> result) {
        if (result == null || !result.hasValue()) {
            return null;
        }
        return KeyValue.of(decodeKey(result.getKey()), decodeScoredValue(result.getValue()));
    }

    private static double normalizeScore(double score) {
        if (score == Double.MIN_VALUE) {
            return Double.NEGATIVE_INFINITY;
        }
        if (score == Double.MAX_VALUE) {
            return Double.POSITIVE_INFINITY;
        }
        return score;
    }

}
