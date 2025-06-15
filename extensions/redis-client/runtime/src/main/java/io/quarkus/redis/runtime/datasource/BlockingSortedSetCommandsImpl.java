
package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.ScanArgs;
import io.quarkus.redis.datasource.SortArgs;
import io.quarkus.redis.datasource.list.KeyValue;
import io.quarkus.redis.datasource.sortedset.Range;
import io.quarkus.redis.datasource.sortedset.ReactiveSortedSetCommands;
import io.quarkus.redis.datasource.sortedset.ScoreRange;
import io.quarkus.redis.datasource.sortedset.ScoredValue;
import io.quarkus.redis.datasource.sortedset.SortedSetCommands;
import io.quarkus.redis.datasource.sortedset.ZAddArgs;
import io.quarkus.redis.datasource.sortedset.ZAggregateArgs;
import io.quarkus.redis.datasource.sortedset.ZRangeArgs;
import io.quarkus.redis.datasource.sortedset.ZScanCursor;

public class BlockingSortedSetCommandsImpl<K, V> extends AbstractRedisCommandGroup implements SortedSetCommands<K, V> {

    private final ReactiveSortedSetCommands<K, V> reactive;

    public BlockingSortedSetCommandsImpl(RedisDataSource ds, ReactiveSortedSetCommands<K, V> reactive,
            Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public boolean zadd(K key, double score, V member) {
        return reactive.zadd(key, score, member).await().atMost(timeout);
    }

    @Override
    public int zadd(K key, Map<V, Double> items) {
        return reactive.zadd(key, items).await().atMost(timeout);
    }

    @SafeVarargs
    @Override
    public final int zadd(K key, ScoredValue<V>... items) {
        return reactive.zadd(key, items).await().atMost(timeout);
    }

    @Override
    public boolean zadd(K key, ZAddArgs zAddArgs, double score, V member) {
        return reactive.zadd(key, zAddArgs, score, member).await().atMost(timeout);
    }

    @Override
    public int zadd(K key, ZAddArgs zAddArgs, Map<V, Double> items) {
        return reactive.zadd(key, zAddArgs, items).await().atMost(timeout);
    }

    @SafeVarargs
    @Override
    public final int zadd(K key, ZAddArgs zAddArgs, ScoredValue<V>... items) {
        return reactive.zadd(key, zAddArgs, items).await().atMost(timeout);
    }

    @Override
    public double zaddincr(K key, double score, V member) {
        return reactive.zaddincr(key, score, member).await().atMost(timeout);
    }

    @Override
    public OptionalDouble zaddincr(K key, ZAddArgs zAddArgs, double score, V member) {
        return reactive.zaddincr(key, zAddArgs, score, member).map(d -> {
            if (d == null) {
                return OptionalDouble.empty();
            }
            return OptionalDouble.of(d);
        }).await().atMost(timeout);
    }

    @Override
    public long zcard(K key) {
        return reactive.zcard(key).await().atMost(timeout);
    }

    @Override
    public long zcount(K key, ScoreRange<Double> range) {
        return reactive.zcount(key, range).await().atMost(timeout);
    }

    @Override
    public List<V> zdiff(K... keys) {
        return reactive.zdiff(keys).await().atMost(timeout);
    }

    @Override
    public List<ScoredValue<V>> zdiffWithScores(K... keys) {
        return reactive.zdiffWithScores(keys).await().atMost(timeout);
    }

    @Override
    public long zdiffstore(K destination, K... keys) {
        return reactive.zdiffstore(destination, keys).await().atMost(timeout);
    }

    @Override
    public double zincrby(K key, double increment, V member) {
        return reactive.zincrby(key, increment, member).await().atMost(timeout);
    }

    @Override
    public List<V> zinter(ZAggregateArgs arguments, K... keys) {
        return reactive.zinter(arguments, keys).await().atMost(timeout);
    }

    @Override
    public List<V> zinter(K... keys) {
        return reactive.zinter(keys).await().atMost(timeout);
    }

    @Override
    public List<ScoredValue<V>> zinterWithScores(ZAggregateArgs arguments, K... keys) {
        return reactive.zinterWithScores(arguments, keys).await().atMost(timeout);
    }

    @Override
    public List<ScoredValue<V>> zinterWithScores(K... keys) {
        return reactive.zinterWithScores(keys).await().atMost(timeout);
    }

    @Override
    public long zintercard(K... keys) {
        return reactive.zintercard(keys).await().atMost(timeout);
    }

    @Override
    public long zintercard(long limit, K... keys) {
        return reactive.zintercard(limit, keys).await().atMost(timeout);
    }

    @Override
    public long zinterstore(K destination, ZAggregateArgs arguments, K... keys) {
        return reactive.zinterstore(destination, arguments, keys).await().atMost(timeout);
    }

    @Override
    public long zinterstore(K destination, K... keys) {
        return reactive.zinterstore(destination, keys).await().atMost(timeout);
    }

    @Override
    public long zlexcount(K key, Range<String> range) {
        return reactive.zlexcount(key, range).await().atMost(timeout);
    }

    @Override
    public ScoredValue<V> zmpopMin(K... keys) {
        return reactive.zmpopMin(keys).await().atMost(timeout);
    }

    @Override
    public List<ScoredValue<V>> zmpopMin(int count, K... keys) {
        return reactive.zmpopMin(count, keys).await().atMost(timeout);
    }

    @Override
    public ScoredValue<V> zmpopMax(K... keys) {
        return reactive.zmpopMax(keys).await().atMost(timeout);
    }

    @Override
    public List<ScoredValue<V>> zmpopMax(int count, K... keys) {
        return reactive.zmpopMax(count, keys).await().atMost(timeout);
    }

    @Override
    public ScoredValue<V> bzmpopMin(Duration timeout, K... keys) {
        return reactive.bzmpopMin(timeout, keys).await().atMost(this.timeout);
    }

    @Override
    public List<ScoredValue<V>> bzmpopMin(Duration timeout, int count, K... keys) {
        return reactive.bzmpopMin(timeout, count, keys).await().atMost(this.timeout);
    }

    @Override
    public ScoredValue<V> bzmpopMax(Duration timeout, K... keys) {
        return reactive.bzmpopMax(timeout, keys).await().atMost(this.timeout);
    }

    @Override
    public List<ScoredValue<V>> bzmpopMax(Duration timeout, int count, K... keys) {
        return reactive.bzmpopMax(timeout, count, keys).await().atMost(this.timeout);
    }

    @Override
    public List<OptionalDouble> zmscore(K key, V... members) {
        return reactive.zmscore(key, members).map(list -> list.stream().map(d -> {
            if (d == null) {
                return OptionalDouble.empty();
            }
            return OptionalDouble.of(d);
        }).collect(Collectors.toList())).await().atMost(timeout);
    }

    @Override
    public ScoredValue<V> zpopmax(K key) {
        return reactive.zpopmax(key).await().atMost(timeout);
    }

    @Override
    public List<ScoredValue<V>> zpopmax(K key, int count) {
        return reactive.zpopmax(key, count).await().atMost(timeout);
    }

    @Override
    public ScoredValue<V> zpopmin(K key) {
        return reactive.zpopmin(key).await().atMost(timeout);
    }

    @Override
    public List<ScoredValue<V>> zpopmin(K key, int count) {
        return reactive.zpopmin(key, count).await().atMost(timeout);
    }

    @Override
    public V zrandmember(K key) {
        return reactive.zrandmember(key).await().atMost(timeout);
    }

    @Override
    public List<V> zrandmember(K key, int count) {
        return reactive.zrandmember(key, count).await().atMost(timeout);
    }

    @Override
    public ScoredValue<V> zrandmemberWithScores(K key) {
        return reactive.zrandmemberWithScores(key).await().atMost(timeout);
    }

    @Override
    public List<ScoredValue<V>> zrandmemberWithScores(K key, int count) {
        return reactive.zrandmemberWithScores(key, count).await().atMost(timeout);
    }

    @Override
    public KeyValue<K, ScoredValue<V>> bzpopmin(Duration timeout, K... keys) {
        return reactive.bzpopmin(timeout, keys).await().atMost(this.timeout);
    }

    @Override
    public KeyValue<K, ScoredValue<V>> bzpopmax(Duration timeout, K... keys) {
        return reactive.bzpopmax(timeout, keys).await().atMost(this.timeout);
    }

    @Override
    public List<V> zrange(K key, long start, long stop, ZRangeArgs args) {
        return reactive.zrange(key, start, stop, args).await().atMost(timeout);
    }

    @Override
    public List<ScoredValue<V>> zrangeWithScores(K key, long start, long stop, ZRangeArgs args) {
        return reactive.zrangeWithScores(key, start, stop, args).await().atMost(timeout);
    }

    @Override
    public List<V> zrange(K key, long start, long stop) {
        return reactive.zrange(key, start, stop).await().atMost(timeout);
    }

    @Override
    public List<ScoredValue<V>> zrangeWithScores(K key, long start, long stop) {
        return reactive.zrangeWithScores(key, start, stop).await().atMost(timeout);
    }

    @Override
    public List<V> zrangebylex(K key, Range<String> range, ZRangeArgs args) {
        return reactive.zrangebylex(key, range, args).await().atMost(timeout);
    }

    @Override
    public List<V> zrangebylex(K key, Range<String> range) {
        return reactive.zrangebylex(key, range).await().atMost(timeout);
    }

    @Override
    public List<V> zrangebyscore(K key, ScoreRange<Double> range, ZRangeArgs args) {
        return reactive.zrangebyscore(key, range, args).await().atMost(timeout);
    }

    @Override
    public List<ScoredValue<V>> zrangebyscoreWithScores(K key, ScoreRange<Double> range, ZRangeArgs args) {
        return reactive.zrangebyscoreWithScores(key, range, args).await().atMost(timeout);
    }

    @Override
    public List<V> zrangebyscore(K key, ScoreRange<Double> range) {
        return reactive.zrangebyscore(key, range).await().atMost(timeout);
    }

    @Override
    public List<ScoredValue<V>> zrangebyscoreWithScores(K key, ScoreRange<Double> range) {
        return reactive.zrangebyscoreWithScores(key, range).await().atMost(timeout);
    }

    @Override
    public long zrangestore(K dst, K src, long min, long max, ZRangeArgs args) {
        return reactive.zrangestore(dst, src, min, max, args).await().atMost(timeout);
    }

    @Override
    public long zrangestore(K dst, K src, long min, long max) {
        return reactive.zrangestore(dst, src, min, max).await().atMost(timeout);
    }

    @Override
    public long zrangestorebylex(K dst, K src, Range<String> range, ZRangeArgs args) {
        return reactive.zrangestorebylex(dst, src, range, args).await().atMost(timeout);
    }

    @Override
    public long zrangestorebylex(K dst, K src, Range<String> range) {
        return reactive.zrangestorebylex(dst, src, range).await().atMost(timeout);
    }

    @Override
    public long zrangestorebyscore(K dst, K src, ScoreRange<Double> range, ZRangeArgs args) {
        return reactive.zrangestorebyscore(dst, src, range, args).await().atMost(timeout);
    }

    @Override
    public long zrangestorebyscore(K dst, K src, ScoreRange<Double> range) {
        return reactive.zrangestorebyscore(dst, src, range).await().atMost(timeout);
    }

    @Override
    public OptionalLong zrank(K key, V member) {
        return reactive.zrank(key, member).map(l -> {
            if (l == null) {
                return OptionalLong.empty();
            } else {
                return OptionalLong.of(l);
            }
        }).await().atMost(timeout);
    }

    @Override
    public int zrem(K key, V... members) {
        return reactive.zrem(key, members).await().atMost(timeout);
    }

    @Override
    public long zremrangebylex(K key, Range<String> range) {
        return reactive.zremrangebylex(key, range).await().atMost(timeout);
    }

    @Override
    public long zremrangebyrank(K key, long start, long stop) {
        return reactive.zremrangebyrank(key, start, stop).await().atMost(timeout);
    }

    @Override
    public long zremrangebyscore(K key, ScoreRange<Double> range) {
        return reactive.zremrangebyscore(key, range).await().atMost(timeout);
    }

    @Override
    public OptionalLong zrevrank(K key, V member) {
        return reactive.zrevrank(key, member).map(l -> {
            if (l == null) {
                return OptionalLong.empty();
            }
            return OptionalLong.of(l);
        }).await().atMost(timeout);
    }

    @Override
    public ZScanCursor<V> zscan(K key) {
        return new ZScanBlockingCursorImpl<>(reactive.zscan(key), timeout);
    }

    @Override
    public ZScanCursor<V> zscan(K key, ScanArgs args) {
        return new ZScanBlockingCursorImpl<>(reactive.zscan(key, args), timeout);
    }

    @Override
    public OptionalDouble zscore(K key, V member) {
        return reactive.zscore(key, member).map(score -> {
            if (score == null) {
                return OptionalDouble.empty();
            }
            return OptionalDouble.of(score);
        }).await().atMost(timeout);
    }

    @Override
    public List<V> zunion(ZAggregateArgs args, K... keys) {
        return reactive.zunion(args, keys).await().atMost(timeout);
    }

    @Override
    public List<V> zunion(K... keys) {
        return reactive.zunion(keys).await().atMost(timeout);
    }

    @Override
    public List<ScoredValue<V>> zunionWithScores(ZAggregateArgs args, K... keys) {
        return reactive.zunionWithScores(args, keys).await().atMost(timeout);
    }

    @Override
    public List<ScoredValue<V>> zunionWithScores(K... keys) {
        return reactive.zunionWithScores(keys).await().atMost(timeout);
    }

    @Override
    public long zunionstore(K destination, ZAggregateArgs args, K... keys) {
        return reactive.zunionstore(destination, args, keys).await().atMost(timeout);
    }

    @Override
    public long zunionstore(K destination, K... keys) {
        return reactive.zunionstore(destination, keys).await().atMost(timeout);
    }

    @Override
    public List<V> sort(K key) {
        return reactive.sort(key).await().atMost(timeout);
    }

    @Override
    public List<V> sort(K key, SortArgs sortArguments) {
        return reactive.sort(key, sortArguments).await().atMost(timeout);
    }

    @Override
    public long sortAndStore(K key, K destination, SortArgs sortArguments) {
        return reactive.sortAndStore(key, destination, sortArguments).await().atMost(timeout);
    }

    @Override
    public long sortAndStore(K key, K destination) {
        return reactive.sortAndStore(key, destination).await().atMost(timeout);
    }

}
