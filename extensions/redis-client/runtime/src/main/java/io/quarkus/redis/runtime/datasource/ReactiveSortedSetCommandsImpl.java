
package io.quarkus.redis.runtime.datasource;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.ScanArgs;
import io.quarkus.redis.datasource.list.KeyValue;
import io.quarkus.redis.datasource.sortedset.Range;
import io.quarkus.redis.datasource.sortedset.ReactiveSortedSetCommands;
import io.quarkus.redis.datasource.sortedset.ReactiveZScanCursor;
import io.quarkus.redis.datasource.sortedset.ScoreRange;
import io.quarkus.redis.datasource.sortedset.ScoredValue;
import io.quarkus.redis.datasource.sortedset.ZAddArgs;
import io.quarkus.redis.datasource.sortedset.ZAggregateArgs;
import io.quarkus.redis.datasource.sortedset.ZRangeArgs;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveSortedSetCommandsImpl<K, V> extends AbstractSortedSetCommands<K, V>
        implements ReactiveSortedSetCommands<K, V> {

    private final ReactiveRedisDataSource reactive;

    public ReactiveSortedSetCommandsImpl(ReactiveRedisDataSourceImpl redis, Type k, Type v) {
        super(redis, k, v);
        this.reactive = redis;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return reactive;
    }

    @Override
    public Uni<Boolean> zadd(K key, double score, V value) {
        return zadd(key, new ZAddArgs(), score, value);
    }

    @Override
    public Uni<Integer> zadd(K key, Map<V, Double> items) {
        return zadd(key, new ZAddArgs(), items);
    }

    @Override
    public Uni<Integer> zadd(K key, ScoredValue<V>... items) {
        return zadd(key, new ZAddArgs(), items);
    }

    @Override
    public Uni<Boolean> zadd(K key, ZAddArgs args, double score, V value) {
        return super._zadd(key, args, score, value).map(this::decodeIntAsBoolean);
    }

    @Override
    public Uni<Integer> zadd(K key, ZAddArgs args, Map<V, Double> items) {
        return super._zadd(key, args, items).map(Response::toInteger);
    }

    @Override
    public Uni<Integer> zadd(K key, ZAddArgs args, ScoredValue<V>... items) {
        return super._zadd(key, args, items).map(Response::toInteger);
    }

    @Override
    public Uni<Double> zaddincr(K key, double score, V value) {
        return zaddincr(key, new ZAddArgs(), score, value);
    }

    @Override
    public Uni<Double> zaddincr(K key, ZAddArgs args, double score, V value) {
        return super._zaddincr(key, args, score, value).map(this::decodeAsDouble);
    }

    @Override
    public Uni<Long> zcard(K key) {
        return super._zcard(key).map(this::decodeLongOrZero);
    }

    @Override
    public Uni<Long> zcount(K key, ScoreRange<Double> range) {
        return super._zcount(key, range).map(Response::toLong);

    }

    @Override
    public Uni<List<V>> zdiff(K... keys) {
        return super._zdiff(keys).map(this::decodeAsListOfValues);
    }

    @Override
    public Uni<List<ScoredValue<V>>> zdiffWithScores(K... keys) {
        return super._zdiffWithScores(keys).map(this::decodeAsListOfScoredValues);
    }

    @Override
    public Uni<Long> zdiffstore(K destination, K... keys) {
        return super._zdiffstore(destination, keys).map(Response::toLong);
    }

    @Override
    public Uni<Double> zincrby(K key, double increment, V value) {
        return super._zincrby(key, increment, value).map(Response::toDouble);
    }

    @Override
    public Uni<List<V>> zinter(ZAggregateArgs args, K... keys) {
        return super._zinter(args, keys).map(r -> marshaller.decodeAsList(r, typeOfValue));
    }

    @Override
    public Uni<List<V>> zinter(K... keys) {
        return zinter(DEFAULT_INSTANCE_AGG, keys);
    }

    @Override
    public Uni<List<ScoredValue<V>>> zinterWithScores(ZAggregateArgs arguments, K... keys) {
        return super._zinterWithScores(arguments, keys).map(this::decodeAsListOfScoredValues);
    }

    @Override
    public Uni<List<ScoredValue<V>>> zinterWithScores(K... keys) {
        return zinterWithScores(DEFAULT_INSTANCE_AGG, keys);
    }

    @Override
    public Uni<Long> zintercard(K... keys) {
        return super._zintercard(keys).map(Response::toLong);
    }

    @Override
    public Uni<Long> zintercard(long limit, K... keys) {
        return super._zintercard(limit, keys).map(Response::toLong);
    }

    @Override
    public Uni<Long> zinterstore(K destination, ZAggregateArgs arguments, K... keys) {
        return super._zinterstore(destination, arguments, keys).map(Response::toLong);
    }

    @SafeVarargs
    @Override
    public final Uni<Long> zinterstore(K destination, K... keys) {
        return zinterstore(destination, DEFAULT_INSTANCE_AGG, keys);
    }

    @Override
    public Uni<Long> zlexcount(K key, Range<String> range) {
        return super._zlexcount(key, range).map(Response::toLong);
    }

    @Override
    public Uni<ScoredValue<V>> zmpopMin(K... keys) {
        return super._zmpopMin(keys).map(this::decodePopResponse);
    }

    @Override
    public Uni<List<ScoredValue<V>>> zmpopMin(int count, K... keys) {
        return super._zmpopMin(count, keys).map(this::decodePopResponseWithCount);
    }

    @Override
    public Uni<ScoredValue<V>> zmpopMax(K... keys) {
        return super._zmpopMax(keys).map(this::decodePopResponse);
    }

    @Override
    public Uni<List<ScoredValue<V>>> zmpopMax(int count, K... keys) {
        return super._zmpopMax(count, keys).map(this::decodePopResponseWithCount);
    }

    @Override
    public Uni<ScoredValue<V>> bzmpopMin(Duration timeout, K... keys) {
        return super._bzmpopMin(timeout, keys).map(this::decodePopResponse);
    }

    @Override
    public Uni<List<ScoredValue<V>>> bzmpopMin(Duration timeout, int count, K... keys) {
        return super._bzmpopMin(timeout, count, keys).map(this::decodePopResponseWithCount);
    }

    @Override
    public Uni<ScoredValue<V>> bzmpopMax(Duration timeout, K... keys) {
        return super._bzmpopMax(timeout, keys).map(this::decodePopResponse);
    }

    @Override
    public Uni<List<ScoredValue<V>>> bzmpopMax(Duration timeout, int count, K... keys) {
        return super._bzmpopMax(timeout, count, keys).map(this::decodePopResponseWithCount);
    }

    @Override
    public Uni<List<Double>> zmscore(K key, V... values) {
        return super._zmscore(key, values).map(this::decodeAsListOfDouble);
    }

    @Override
    public Uni<ScoredValue<V>> zpopmax(K key) {
        return super._zpopmax(key).map(this::decodeAsScoredValue);
    }

    @Override
    public Uni<List<ScoredValue<V>>> zpopmax(K key, int count) {
        return super._zpopmax(key, count).map(this::decodeAsListOfScoredValues);
    }

    @Override
    public Uni<ScoredValue<V>> zpopmin(K key) {
        return super._zpopmin(key).map(this::decodeAsScoredValue);
    }

    @Override
    public Uni<List<ScoredValue<V>>> zpopmin(K key, int count) {
        return super._zpopmin(key, count).map(this::decodeAsListOfScoredValues);
    }

    @Override
    public Uni<V> zrandmember(K key) {
        return super._zrandmember(key).map(this::decodeV);
    }

    @Override
    public Uni<List<V>> zrandmember(K key, int count) {
        return super._zrandmember(key, count).map(this::decodeAsListOfValues);
    }

    @Override
    public Uni<ScoredValue<V>> zrandmemberWithScores(K key) {
        return super._zrandmemberWithScores(key).map(this::decodeAsScoredValueOrEmpty);
    }

    @Override
    public Uni<List<ScoredValue<V>>> zrandmemberWithScores(K key, int count) {
        return super._zrandmemberWithScores(key, count).map(this::decodeAsListOfScoredValues);
    }

    @SafeVarargs
    @Override
    public final Uni<KeyValue<K, ScoredValue<V>>> bzpopmin(Duration timeout, K... keys) {
        return super._bzpopmin(timeout, keys).map(this::decodeAsKeyValue);
    }

    @SafeVarargs
    @Override
    public final Uni<KeyValue<K, ScoredValue<V>>> bzpopmax(Duration timeout, K... keys) {
        return super._bzpopmax(timeout, keys).map(this::decodeAsKeyValue);
    }

    @Override
    public Uni<List<V>> zrange(K key, long start, long stop, ZRangeArgs args) {
        return super._zrange(key, start, stop, args).map(this::decodeAsListOfValues);
    }

    @Override
    public Uni<List<ScoredValue<V>>> zrangeWithScores(K key, long start, long stop, ZRangeArgs args) {
        return super._zrangeWithScores(key, start, stop, args).map(this::decodeAsListOfScoredValues);
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
        return super._zrangebylex(key, range, args).map(this::decodeAsListOfValues);
    }

    @Override
    public Uni<List<V>> zrangebylex(K key, Range<String> range) {
        return zrangebylex(key, range, DEFAULT_INSTANCE_RANGE);
    }

    @Override
    public Uni<List<V>> zrangebyscore(K key, ScoreRange<Double> range, ZRangeArgs args) {
        return super._zrangebyscore(key, range, args).map(this::decodeAsListOfValues);
    }

    @Override
    public Uni<List<ScoredValue<V>>> zrangebyscoreWithScores(K key, ScoreRange<Double> range, ZRangeArgs args) {
        return super._zrangebyscoreWithScores(key, range, args).map(this::decodeAsListOfScoredValues);
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
        return super._zrangestore(dst, src, min, max, args).map(Response::toLong);
    }

    @Override
    public Uni<Long> zrangestore(K dst, K src, long min, long max) {
        return zrangestore(dst, src, min, max, DEFAULT_INSTANCE_RANGE);
    }

    @Override
    public Uni<Long> zrangestorebylex(K dst, K src, Range<String> range, ZRangeArgs args) {
        return super._zrangestorebylex(dst, src, range, args).map(Response::toLong);
    }

    @Override
    public Uni<Long> zrangestorebylex(K dst, K src, Range<String> range) {
        return zrangestorebylex(dst, src, range, DEFAULT_INSTANCE_RANGE);
    }

    @Override
    public Uni<Long> zrangestorebyscore(K dst, K src, ScoreRange<Double> range, ZRangeArgs args) {
        return super._zrangestorebyscore(dst, src, range, args).map(Response::toLong);
    }

    @Override
    public Uni<Long> zrangestorebyscore(K dst, K src, ScoreRange<Double> range) {
        return zrangestorebyscore(dst, src, range, DEFAULT_INSTANCE_RANGE);
    }

    @Override
    public Uni<Long> zrank(K key, V value) {
        return super._zrank(key, value).map(this::decodeAsLong);
    }

    @Override
    public Uni<Integer> zrem(K key, V... values) {
        return super._zrem(key, values).map(Response::toInteger);
    }

    @Override
    public Uni<Long> zremrangebylex(K key, Range<String> range) {
        return super._zremrangebylex(key, range).map(Response::toLong);
    }

    @Override
    public Uni<Long> zremrangebyrank(K key, long start, long stop) {
        return super._zremrangebyrank(key, start, stop).map(Response::toLong);
    }

    @Override
    public Uni<Long> zremrangebyscore(K key, ScoreRange<Double> range) {
        return super._zremrangebyscore(key, range).map(Response::toLong);
    }

    @Override
    public Uni<Long> zrevrank(K key, V value) {
        return super._zrevrank(key, value).map(this::decodeAsLong);
    }

    @Override
    public ReactiveZScanCursor<V> zscan(K key) {
        nonNull(key, "key");
        return new ZScanReactiveCursorImpl<>(redis, key, marshaller, typeOfValue, Collections.emptyList());
    }

    @Override
    public ReactiveZScanCursor<V> zscan(K key, ScanArgs args) {
        nonNull(key, "key");
        nonNull(args, "args");
        return new ZScanReactiveCursorImpl<>(redis, key, marshaller, typeOfValue, args.toArgs());
    }

    @Override
    public Uni<Double> zscore(K key, V value) {
        return super._zscore(key, value).map(this::decodeAsDouble);
    }

    @Override
    public Uni<List<V>> zunion(ZAggregateArgs args, K... keys) {
        return super._zunion(args, keys).map(this::decodeAsListOfValues);
    }

    @Override
    public Uni<List<V>> zunion(K... keys) {
        return zunion(DEFAULT_INSTANCE_AGG, keys);
    }

    @Override
    public Uni<List<ScoredValue<V>>> zunionWithScores(K... keys) {
        return zunionWithScores(DEFAULT_INSTANCE_AGG, keys);
    }

    @Override
    public Uni<List<ScoredValue<V>>> zunionWithScores(ZAggregateArgs args, K... keys) {
        return super._zunionWithScores(args, keys).map(this::decodeAsListOfScoredValues);
    }

    @Override
    public Uni<Long> zunionstore(K destination, ZAggregateArgs args, K... keys) {
        return super._zunionstore(destination, args, keys).map(Response::toLong);
    }

    @Override
    public Uni<Long> zunionstore(K destination, K... keys) {
        return zunionstore(destination, DEFAULT_INSTANCE_AGG, keys);
    }

}
