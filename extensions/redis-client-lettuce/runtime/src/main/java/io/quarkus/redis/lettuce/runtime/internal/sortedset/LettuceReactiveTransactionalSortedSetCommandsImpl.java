package io.quarkus.redis.lettuce.runtime.internal.sortedset;

import java.time.Duration;
import java.util.Map;

import io.quarkus.redis.datasource.sortedset.Range;
import io.quarkus.redis.datasource.sortedset.ReactiveTransactionalSortedSetCommands;
import io.quarkus.redis.datasource.sortedset.ScoreRange;
import io.quarkus.redis.datasource.sortedset.ScoredValue;
import io.quarkus.redis.datasource.sortedset.ZAddArgs;
import io.quarkus.redis.datasource.sortedset.ZAggregateArgs;
import io.quarkus.redis.datasource.sortedset.ZRangeArgs;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.quarkus.redis.lettuce.runtime.internal.datasource.LettuceTransactionHolder;
import io.smallrye.mutiny.Uni;

/**
 * Lettuce-backed implementation of {@link ReactiveTransactionalSortedSetCommands}.
 * <p>
 * A thin transactional shell over {@link LettuceReactiveSortedSetCommandsImpl}. Each command reuses the
 * non-transactional command-builder seam ({@code reactive._zxxx(...)}) for validation and argument
 * conversion, and hands the resulting {@link io.quarkus.redis.lettuce.runtime.internal.LettuceCommand}
 * to the {@link LettuceTransactionHolder}. The command carries the same result mapper the
 * non-transactional path applies, so {@code TransactionResult.get(index)} yields the same Java type
 * that command returns.
 *
 * @param <K> the key type
 * @param <V> the type of the scored member
 */
public class LettuceReactiveTransactionalSortedSetCommandsImpl<K, V>
        implements ReactiveTransactionalSortedSetCommands<K, V> {

    private final ReactiveTransactionalRedisDataSource dataSource;
    private final LettuceReactiveSortedSetCommandsImpl<K, V> reactive;
    private final LettuceTransactionHolder tx;

    public LettuceReactiveTransactionalSortedSetCommandsImpl(ReactiveTransactionalRedisDataSource dataSource,
            LettuceReactiveSortedSetCommandsImpl<K, V> reactive,
            LettuceTransactionHolder tx) {
        this.dataSource = dataSource;
        this.reactive = reactive;
        this.tx = tx;
    }

    @Override
    public ReactiveTransactionalRedisDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Uni<Void> zadd(K key, double score, V member) {
        return tx.enqueue(reactive._zadd(key, score, member));
    }

    @Override
    public Uni<Void> zadd(K key, Map<V, Double> items) {
        return tx.enqueue(reactive._zadd(key, items));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> zadd(K key, ScoredValue<V>... items) {
        return tx.enqueue(reactive._zadd(key, items));
    }

    @Override
    public Uni<Void> zadd(K key, ZAddArgs zAddArgs, double score, V member) {
        return tx.enqueue(reactive._zadd(key, zAddArgs, score, member));
    }

    @Override
    public Uni<Void> zadd(K key, ZAddArgs zAddArgs, Map<V, Double> items) {
        return tx.enqueue(reactive._zadd(key, zAddArgs, items));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> zadd(K key, ZAddArgs zAddArgs, ScoredValue<V>... items) {
        return tx.enqueue(reactive._zadd(key, zAddArgs, items));
    }

    @Override
    public Uni<Void> zaddincr(K key, double score, V member) {
        return tx.enqueue(reactive._zaddincr(key, score, member));
    }

    @Override
    public Uni<Void> zaddincr(K key, ZAddArgs zAddArgs, double score, V member) {
        return tx.enqueue(reactive._zaddincr(key, zAddArgs, score, member));
    }

    @Override
    public Uni<Void> zcard(K key) {
        return tx.enqueue(reactive._zcard(key));
    }

    @Override
    public Uni<Void> zcount(K key, ScoreRange<Double> range) {
        return tx.enqueue(reactive._zcount(key, range));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> zdiff(K... keys) {
        return tx.enqueue(reactive._zdiff(keys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> zdiffWithScores(K... keys) {
        return tx.enqueue(reactive._zdiffWithScores(keys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> zdiffstore(K destination, K... keys) {
        return tx.enqueue(reactive._zdiffstore(destination, keys));
    }

    @Override
    public Uni<Void> zincrby(K key, double increment, V member) {
        return tx.enqueue(reactive._zincrby(key, increment, member));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> zinter(ZAggregateArgs arguments, K... keys) {
        return tx.enqueue(reactive._zinter(arguments, keys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> zinter(K... keys) {
        return tx.enqueue(reactive._zinter(keys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> zinterWithScores(ZAggregateArgs arguments, K... keys) {
        return tx.enqueue(reactive._zinterWithScores(arguments, keys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> zinterWithScores(K... keys) {
        return tx.enqueue(reactive._zinterWithScores(keys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> zintercard(K... keys) {
        return tx.enqueue(reactive._zintercard(keys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> zintercard(long limit, K... keys) {
        return tx.enqueue(reactive._zintercard(limit, keys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> zinterstore(K destination, ZAggregateArgs arguments, K... keys) {
        return tx.enqueue(reactive._zinterstore(destination, arguments, keys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> zinterstore(K destination, K... keys) {
        return tx.enqueue(reactive._zinterstore(destination, keys));
    }

    @Override
    public Uni<Void> zlexcount(K key, Range<String> range) {
        return tx.enqueue(reactive._zlexcount(key, range));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> zmpopMin(K... keys) {
        return tx.enqueue(reactive._zmpopMin(keys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> zmpopMin(int count, K... keys) {
        return tx.enqueue(reactive._zmpopMin(count, keys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> zmpopMax(K... keys) {
        return tx.enqueue(reactive._zmpopMax(keys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> zmpopMax(int count, K... keys) {
        return tx.enqueue(reactive._zmpopMax(count, keys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> bzmpopMin(Duration timeout, K... keys) {
        return tx.enqueue(reactive._bzmpopMin(timeout, keys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> bzmpopMin(Duration timeout, int count, K... keys) {
        return tx.enqueue(reactive._bzmpopMin(timeout, count, keys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> bzmpopMax(Duration timeout, K... keys) {
        return tx.enqueue(reactive._bzmpopMax(timeout, keys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> bzmpopMax(Duration timeout, int count, K... keys) {
        return tx.enqueue(reactive._bzmpopMax(timeout, count, keys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> zmscore(K key, V... members) {
        return tx.enqueue(reactive._zmscore(key, members));
    }

    @Override
    public Uni<Void> zpopmax(K key) {
        return tx.enqueue(reactive._zpopmax(key));
    }

    @Override
    public Uni<Void> zpopmax(K key, int count) {
        return tx.enqueue(reactive._zpopmax(key, count));
    }

    @Override
    public Uni<Void> zpopmin(K key) {
        return tx.enqueue(reactive._zpopmin(key));
    }

    @Override
    public Uni<Void> zpopmin(K key, int count) {
        return tx.enqueue(reactive._zpopmin(key, count));
    }

    @Override
    public Uni<Void> zrandmember(K key) {
        return tx.enqueue(reactive._zrandmember(key));
    }

    @Override
    public Uni<Void> zrandmember(K key, int count) {
        return tx.enqueue(reactive._zrandmember(key, count));
    }

    @Override
    public Uni<Void> zrandmemberWithScores(K key) {
        return tx.enqueue(reactive._zrandmemberWithScores(key));
    }

    @Override
    public Uni<Void> zrandmemberWithScores(K key, int count) {
        return tx.enqueue(reactive._zrandmemberWithScores(key, count));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> bzpopmin(Duration timeout, K... keys) {
        return tx.enqueue(reactive._bzpopmin(timeout, keys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> bzpopmax(Duration timeout, K... keys) {
        return tx.enqueue(reactive._bzpopmax(timeout, keys));
    }

    @Override
    public Uni<Void> zrange(K key, long start, long stop, ZRangeArgs args) {
        return tx.enqueue(reactive._zrange(key, start, stop, args));
    }

    @Override
    public Uni<Void> zrangeWithScores(K key, long start, long stop, ZRangeArgs args) {
        return tx.enqueue(reactive._zrangeWithScores(key, start, stop, args));
    }

    @Override
    public Uni<Void> zrange(K key, long start, long stop) {
        return tx.enqueue(reactive._zrange(key, start, stop));
    }

    @Override
    public Uni<Void> zrangeWithScores(K key, long start, long stop) {
        return tx.enqueue(reactive._zrangeWithScores(key, start, stop));
    }

    @Override
    public Uni<Void> zrangebylex(K key, Range<String> range, ZRangeArgs args) {
        return tx.enqueue(reactive._zrangebylex(key, range, args));
    }

    @Override
    public Uni<Void> zrangebylex(K key, Range<String> range) {
        return tx.enqueue(reactive._zrangebylex(key, range));
    }

    @Override
    public Uni<Void> zrangebyscore(K key, ScoreRange<Double> range, ZRangeArgs args) {
        return tx.enqueue(reactive._zrangebyscore(key, range, args));
    }

    @Override
    public Uni<Void> zrangebyscoreWithScores(K key, ScoreRange<Double> range, ZRangeArgs args) {
        return tx.enqueue(reactive._zrangebyscoreWithScores(key, range, args));
    }

    @Override
    public Uni<Void> zrangebyscore(K key, ScoreRange<Double> range) {
        return tx.enqueue(reactive._zrangebyscore(key, range));
    }

    @Override
    public Uni<Void> zrangebyscoreWithScores(K key, ScoreRange<Double> range) {
        return tx.enqueue(reactive._zrangebyscoreWithScores(key, range));
    }

    @Override
    public Uni<Void> zrangestore(K dst, K src, long min, long max, ZRangeArgs args) {
        return tx.enqueue(reactive._zrangestore(dst, src, min, max, args));
    }

    @Override
    public Uni<Void> zrangestore(K dst, K src, long min, long max) {
        return tx.enqueue(reactive._zrangestore(dst, src, min, max));
    }

    @Override
    public Uni<Void> zrangestorebylex(K dst, K src, Range<String> range, ZRangeArgs args) {
        return tx.enqueue(reactive._zrangestorebylex(dst, src, range, args));
    }

    @Override
    public Uni<Void> zrangestorebylex(K dst, K src, Range<String> range) {
        return tx.enqueue(reactive._zrangestorebylex(dst, src, range));
    }

    @Override
    public Uni<Void> zrangestorebyscore(K dst, K src, ScoreRange<Double> range, ZRangeArgs args) {
        return tx.enqueue(reactive._zrangestorebyscore(dst, src, range, args));
    }

    @Override
    public Uni<Void> zrangestorebyscore(K dst, K src, ScoreRange<Double> range) {
        return tx.enqueue(reactive._zrangestorebyscore(dst, src, range));
    }

    @Override
    public Uni<Void> zrank(K key, V member) {
        return tx.enqueue(reactive._zrank(key, member));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> zrem(K key, V... members) {
        return tx.enqueue(reactive._zrem(key, members));
    }

    @Override
    public Uni<Void> zremrangebylex(K key, Range<String> range) {
        return tx.enqueue(reactive._zremrangebylex(key, range));
    }

    @Override
    public Uni<Void> zremrangebyrank(K key, long start, long stop) {
        return tx.enqueue(reactive._zremrangebyrank(key, start, stop));
    }

    @Override
    public Uni<Void> zremrangebyscore(K key, ScoreRange<Double> range) {
        return tx.enqueue(reactive._zremrangebyscore(key, range));
    }

    @Override
    public Uni<Void> zrevrank(K key, V member) {
        return tx.enqueue(reactive._zrevrank(key, member));
    }

    @Override
    public Uni<Void> zscore(K key, V member) {
        return tx.enqueue(reactive._zscore(key, member));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> zunion(ZAggregateArgs args, K... keys) {
        return tx.enqueue(reactive._zunion(args, keys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> zunion(K... keys) {
        return tx.enqueue(reactive._zunion(keys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> zunionWithScores(ZAggregateArgs args, K... keys) {
        return tx.enqueue(reactive._zunionWithScores(args, keys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> zunionWithScores(K... keys) {
        return tx.enqueue(reactive._zunionWithScores(keys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> zunionstore(K destination, ZAggregateArgs args, K... keys) {
        return tx.enqueue(reactive._zunionstore(destination, args, keys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> zunionstore(K destination, K... keys) {
        return tx.enqueue(reactive._zunionstore(destination, keys));
    }

}
