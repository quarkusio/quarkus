package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.util.Map;

import io.quarkus.redis.datasource.sortedset.Range;
import io.quarkus.redis.datasource.sortedset.ReactiveTransactionalSortedSetCommands;
import io.quarkus.redis.datasource.sortedset.ScoreRange;
import io.quarkus.redis.datasource.sortedset.ScoredValue;
import io.quarkus.redis.datasource.sortedset.TransactionalSortedSetCommands;
import io.quarkus.redis.datasource.sortedset.ZAddArgs;
import io.quarkus.redis.datasource.sortedset.ZAggregateArgs;
import io.quarkus.redis.datasource.sortedset.ZRangeArgs;
import io.quarkus.redis.datasource.transactions.TransactionalRedisDataSource;

public class BlockingTransactionalSortedSetCommandsImpl<K, V> extends AbstractTransactionalRedisCommandGroup
        implements TransactionalSortedSetCommands<K, V> {

    private final ReactiveTransactionalSortedSetCommands<K, V> reactive;

    public BlockingTransactionalSortedSetCommandsImpl(TransactionalRedisDataSource ds,
            ReactiveTransactionalSortedSetCommands<K, V> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public void zadd(K key, double score, V member) {
        this.reactive.zadd(key, score, member).await().atMost(this.timeout);
    }

    @Override
    public void zadd(K key, Map<V, Double> items) {
        this.reactive.zadd(key, items).await().atMost(this.timeout);
    }

    @Override
    public void zadd(K key, ScoredValue<V>... items) {
        this.reactive.zadd(key, items).await().atMost(this.timeout);
    }

    @Override
    public void zadd(K key, ZAddArgs zAddArgs, double score, V member) {
        this.reactive.zadd(key, zAddArgs, score, member).await().atMost(this.timeout);
    }

    @Override
    public void zadd(K key, ZAddArgs zAddArgs, Map<V, Double> items) {
        this.reactive.zadd(key, zAddArgs, items).await().atMost(this.timeout);
    }

    @Override
    public void zadd(K key, ZAddArgs zAddArgs, ScoredValue<V>... items) {
        this.reactive.zadd(key, zAddArgs, items).await().atMost(this.timeout);
    }

    @Override
    public void zaddincr(K key, double score, V member) {
        this.reactive.zaddincr(key, score, member).await().atMost(this.timeout);
    }

    @Override
    public void zaddincr(K key, ZAddArgs zAddArgs, double score, V member) {
        this.reactive.zaddincr(key, zAddArgs, score, member).await().atMost(this.timeout);
    }

    @Override
    public void zcard(K key) {
        this.reactive.zcard(key).await().atMost(this.timeout);
    }

    @Override
    public void zcount(K key, ScoreRange<Double> range) {
        this.reactive.zcount(key, range).await().atMost(this.timeout);
    }

    @Override
    public void zdiff(K... keys) {
        this.reactive.zdiff(keys).await().atMost(this.timeout);
    }

    @Override
    public void zdiffWithScores(K... keys) {
        this.reactive.zdiffWithScores(keys).await().atMost(this.timeout);
    }

    @Override
    public void zdiffstore(K destination, K... keys) {
        this.reactive.zdiffstore(destination, keys).await().atMost(this.timeout);
    }

    @Override
    public void zincrby(K key, double increment, V member) {
        this.reactive.zincrby(key, increment, member).await().atMost(this.timeout);
    }

    @Override
    public void zinter(ZAggregateArgs arguments, K... keys) {
        this.reactive.zinter(arguments, keys).await().atMost(this.timeout);
    }

    @Override
    public void zinter(K... keys) {
        this.reactive.zinter(keys).await().atMost(this.timeout);
    }

    @Override
    public void zinterWithScores(ZAggregateArgs arguments, K... keys) {
        this.reactive.zinterWithScores(arguments, keys).await().atMost(this.timeout);
    }

    @Override
    public void zinterWithScores(K... keys) {
        this.reactive.zinterWithScores(keys).await().atMost(this.timeout);
    }

    @Override
    public void zintercard(K... keys) {
        this.reactive.zintercard(keys).await().atMost(this.timeout);
    }

    @Override
    public void zintercard(long limit, K... keys) {
        this.reactive.zintercard(limit, keys).await().atMost(this.timeout);
    }

    @Override
    public void zinterstore(K destination, ZAggregateArgs arguments, K... keys) {
        this.reactive.zinterstore(destination, arguments, keys).await().atMost(this.timeout);
    }

    @Override
    public void zinterstore(K destination, K... keys) {
        this.reactive.zinterstore(destination, keys).await().atMost(this.timeout);
    }

    @Override
    public void zlexcount(K key, Range<String> range) {
        this.reactive.zlexcount(key, range).await().atMost(this.timeout);
    }

    @Override
    public void zmpopMin(K... keys) {
        this.reactive.zmpopMin(keys).await().atMost(this.timeout);
    }

    @Override
    public void zmpopMin(int count, K... keys) {
        this.reactive.zmpopMin(count, keys).await().atMost(this.timeout);
    }

    @Override
    public void zmpopMax(K... keys) {
        this.reactive.zmpopMax(keys).await().atMost(this.timeout);
    }

    @Override
    public void zmpopMax(int count, K... keys) {
        this.reactive.zmpopMax(count, keys).await().atMost(this.timeout);
    }

    @Override
    public void bzmpopMin(Duration timeout, K... keys) {
        this.reactive.bzmpopMin(timeout, keys).await().atMost(this.timeout);
    }

    @Override
    public void bzmpopMin(Duration timeout, int count, K... keys) {
        this.reactive.bzmpopMin(timeout, count, keys).await().atMost(this.timeout);
    }

    @Override
    public void bzmpopMax(Duration timeout, K... keys) {
        this.reactive.bzmpopMax(timeout, keys).await().atMost(this.timeout);
    }

    @Override
    public void bzmpopMax(Duration timeout, int count, K... keys) {
        this.reactive.bzmpopMax(timeout, count, keys).await().atMost(this.timeout);
    }

    @Override
    public void zmscore(K key, V... members) {
        this.reactive.zmscore(key, members).await().atMost(this.timeout);
    }

    @Override
    public void zpopmax(K key) {
        this.reactive.zpopmax(key).await().atMost(this.timeout);
    }

    @Override
    public void zpopmax(K key, int count) {
        this.reactive.zpopmax(key, count).await().atMost(this.timeout);
    }

    @Override
    public void zpopmin(K key) {
        this.reactive.zpopmin(key).await().atMost(this.timeout);
    }

    @Override
    public void zpopmin(K key, int count) {
        this.reactive.zpopmin(key, count).await().atMost(this.timeout);
    }

    @Override
    public void zrandmember(K key) {
        this.reactive.zrandmember(key).await().atMost(this.timeout);
    }

    @Override
    public void zrandmember(K key, int count) {
        this.reactive.zrandmember(key, count).await().atMost(this.timeout);
    }

    @Override
    public void zrandmemberWithScores(K key) {
        this.reactive.zrandmemberWithScores(key).await().atMost(this.timeout);
    }

    @Override
    public void zrandmemberWithScores(K key, int count) {
        this.reactive.zrandmemberWithScores(key, count).await().atMost(this.timeout);
    }

    @Override
    public void bzpopmin(Duration timeout, K... keys) {
        this.reactive.bzpopmin(timeout, keys).await().atMost(this.timeout);
    }

    @Override
    public void bzpopmax(Duration timeout, K... keys) {
        this.reactive.bzpopmax(timeout, keys).await().atMost(this.timeout);
    }

    @Override
    public void zrange(K key, long start, long stop, ZRangeArgs args) {
        this.reactive.zrange(key, start, stop, args).await().atMost(this.timeout);
    }

    @Override
    public void zrangeWithScores(K key, long start, long stop, ZRangeArgs args) {
        this.reactive.zrangeWithScores(key, start, stop, args).await().atMost(this.timeout);
    }

    @Override
    public void zrange(K key, long start, long stop) {
        this.reactive.zrange(key, start, stop).await().atMost(this.timeout);
    }

    @Override
    public void zrangeWithScores(K key, long start, long stop) {
        this.reactive.zrangeWithScores(key, start, stop).await().atMost(this.timeout);
    }

    @Override
    public void zrangebylex(K key, Range<String> range, ZRangeArgs args) {
        this.reactive.zrangebylex(key, range, args).await().atMost(this.timeout);
    }

    @Override
    public void zrangebylex(K key, Range<String> range) {
        this.reactive.zrangebylex(key, range).await().atMost(this.timeout);
    }

    @Override
    public void zrangebyscore(K key, ScoreRange<Double> range, ZRangeArgs args) {
        this.reactive.zrangebyscore(key, range, args).await().atMost(this.timeout);
    }

    @Override
    public void zrangebyscoreWithScores(K key, ScoreRange<Double> range, ZRangeArgs args) {
        this.reactive.zrangebyscoreWithScores(key, range, args).await().atMost(this.timeout);
    }

    @Override
    public void zrangebyscore(K key, ScoreRange<Double> range) {
        this.reactive.zrangebyscore(key, range).await().atMost(this.timeout);
    }

    @Override
    public void zrangebyscoreWithScores(K key, ScoreRange<Double> range) {
        this.reactive.zrangebyscoreWithScores(key, range).await().atMost(this.timeout);
    }

    @Override
    public void zrangestore(K dst, K src, long min, long max, ZRangeArgs args) {
        this.reactive.zrangestore(dst, src, min, max, args).await().atMost(this.timeout);
    }

    @Override
    public void zrangestore(K dst, K src, long min, long max) {
        this.reactive.zrangestore(dst, src, min, max).await().atMost(this.timeout);
    }

    @Override
    public void zrangestorebylex(K dst, K src, Range<String> range, ZRangeArgs args) {
        this.reactive.zrangestorebylex(dst, src, range, args).await().atMost(this.timeout);
    }

    @Override
    public void zrangestorebylex(K dst, K src, Range<String> range) {
        this.reactive.zrangestorebylex(dst, src, range).await().atMost(this.timeout);
    }

    @Override
    public void zrangestorebyscore(K dst, K src, ScoreRange<Double> range, ZRangeArgs args) {
        this.reactive.zrangestorebyscore(dst, src, range, args).await().atMost(this.timeout);
    }

    @Override
    public void zrangestorebyscore(K dst, K src, ScoreRange<Double> range) {
        this.reactive.zrangestorebyscore(dst, src, range).await().atMost(this.timeout);
    }

    @Override
    public void zrank(K key, V member) {
        this.reactive.zrank(key, member).await().atMost(this.timeout);
    }

    @Override
    public void zrem(K key, V... members) {
        this.reactive.zrem(key, members).await().atMost(this.timeout);
    }

    @Override
    public void zremrangebylex(K key, Range<String> range) {
        this.reactive.zremrangebylex(key, range).await().atMost(this.timeout);
    }

    @Override
    public void zremrangebyrank(K key, long start, long stop) {
        this.reactive.zremrangebyrank(key, start, stop).await().atMost(this.timeout);
    }

    @Override
    public void zremrangebyscore(K key, ScoreRange<Double> range) {
        this.reactive.zremrangebyscore(key, range).await().atMost(this.timeout);
    }

    @Override
    public void zrevrank(K key, V member) {
        this.reactive.zrevrank(key, member).await().atMost(this.timeout);
    }

    @Override
    public void zscore(K key, V member) {
        this.reactive.zscore(key, member).await().atMost(this.timeout);
    }

    @Override
    public void zunion(ZAggregateArgs args, K... keys) {
        this.reactive.zunion(args, keys).await().atMost(this.timeout);
    }

    @Override
    public void zunion(K... keys) {
        this.reactive.zunion(keys).await().atMost(this.timeout);
    }

    @Override
    public void zunionWithScores(ZAggregateArgs args, K... keys) {
        this.reactive.zunionWithScores(args, keys).await().atMost(this.timeout);
    }

    @Override
    public void zunionWithScores(K... keys) {
        this.reactive.zunionWithScores(keys).await().atMost(this.timeout);
    }

    @Override
    public void zunionstore(K destination, ZAggregateArgs args, K... keys) {
        this.reactive.zunionstore(destination, args, keys).await().atMost(this.timeout);
    }

    @Override
    public void zunionstore(K destination, K... keys) {
        this.reactive.zunionstore(destination, keys).await().atMost(this.timeout);
    }
}
