package io.quarkus.redis.runtime.datasource;

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
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveTransactionalSortedSetCommandsImpl<K, V> extends AbstractTransactionalCommands
        implements ReactiveTransactionalSortedSetCommands<K, V> {

    private final ReactiveSortedSetCommandsImpl<K, V> reactive;

    public ReactiveTransactionalSortedSetCommandsImpl(ReactiveTransactionalRedisDataSource ds,
            ReactiveSortedSetCommandsImpl<K, V> reactive, TransactionHolder tx) {
        super(ds, tx);
        this.reactive = reactive;
    }

    @Override
    public Uni<Void> zadd(K key, double score, V member) {
        this.tx.enqueue(reactive::decodeIntAsBoolean);
        return this.reactive._zadd(key, score, member).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zadd(K key, Map<V, Double> items) {
        this.tx.enqueue(Response::toInteger);
        return this.reactive._zadd(key, items).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zadd(K key, ScoredValue<V>... items) {
        this.tx.enqueue(Response::toInteger);
        return this.reactive._zadd(key, items).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zadd(K key, ZAddArgs zAddArgs, double score, V member) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._zadd(key, zAddArgs, score, member).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zadd(K key, ZAddArgs zAddArgs, Map<V, Double> items) {
        this.tx.enqueue(Response::toInteger);
        return this.reactive._zadd(key, zAddArgs, items).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zadd(K key, ZAddArgs zAddArgs, ScoredValue<V>... items) {
        this.tx.enqueue(Response::toInteger);
        return this.reactive._zadd(key, zAddArgs, items).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zaddincr(K key, double score, V member) {
        this.tx.enqueue(Response::toDouble);
        return this.reactive._zaddincr(key, score, member).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zaddincr(K key, ZAddArgs zAddArgs, double score, V member) {
        this.tx.enqueue(this.reactive::decodeAsDouble);
        return this.reactive._zaddincr(key, zAddArgs, score, member).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zcard(K key) {
        this.tx.enqueue(reactive::decodeLongOrZero);
        return this.reactive._zcard(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zcount(K key, ScoreRange<Double> range) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._zcount(key, range).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zdiff(K... keys) {
        this.tx.enqueue(this.reactive::decodeAsListOfValues);
        return this.reactive._zdiff(keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zdiffWithScores(K... keys) {
        this.tx.enqueue(this.reactive::decodeAsListOfScoredValues);
        return this.reactive._zdiffWithScores(keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zdiffstore(K destination, K... keys) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._zdiffstore(destination, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zincrby(K key, double increment, V member) {
        this.tx.enqueue(Response::toDouble);
        return this.reactive._zincrby(key, increment, member).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zinter(ZAggregateArgs arguments, K... keys) {
        this.tx.enqueue(reactive::decodeAsListOfValues);
        return this.reactive._zinter(arguments, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zinter(K... keys) {
        this.tx.enqueue(this.reactive::decodeAsListOfValues);
        return this.reactive._zinter(keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zinterWithScores(ZAggregateArgs arguments, K... keys) {
        this.tx.enqueue(this.reactive::decodeAsListOfScoredValues);
        return this.reactive._zinterWithScores(arguments, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zinterWithScores(K... keys) {
        this.tx.enqueue(this.reactive::decodeAsListOfScoredValues);
        return this.reactive._zinterWithScores(keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zintercard(K... keys) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._zintercard(keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zintercard(long limit, K... keys) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._zintercard(limit, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zinterstore(K destination, ZAggregateArgs arguments, K... keys) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._zinterstore(destination, arguments, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zinterstore(K destination, K... keys) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._zinterstore(destination, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zlexcount(K key, Range<String> range) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._zlexcount(key, range).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zmpopMin(K... keys) {
        this.tx.enqueue(this.reactive::decodePopResponse);
        return this.reactive._zmpopMin(keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zmpopMin(int count, K... keys) {
        this.tx.enqueue(this.reactive::decodePopResponseWithCount);
        return this.reactive._zmpopMin(count, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zmpopMax(K... keys) {
        this.tx.enqueue(this.reactive::decodePopResponse);
        return this.reactive._zmpopMax(keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zmpopMax(int count, K... keys) {
        this.tx.enqueue(this.reactive::decodePopResponseWithCount);
        return this.reactive._zmpopMax(count, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> bzmpopMin(Duration timeout, K... keys) {
        this.tx.enqueue(this.reactive::decodePopResponse);
        return this.reactive._bzmpopMin(timeout, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> bzmpopMin(Duration timeout, int count, K... keys) {
        this.tx.enqueue(this.reactive::decodePopResponseWithCount);
        return this.reactive._bzmpopMin(timeout, count, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> bzmpopMax(Duration timeout, K... keys) {
        this.tx.enqueue(this.reactive::decodePopResponse);
        return this.reactive._bzmpopMax(timeout, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> bzmpopMax(Duration timeout, int count, K... keys) {
        this.tx.enqueue(this.reactive::decodePopResponseWithCount);
        return this.reactive._bzmpopMax(timeout, count, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zmscore(K key, V... members) {
        this.tx.enqueue(this.reactive::decodeAsListOfDouble);
        return this.reactive._zmscore(key, members).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zpopmax(K key) {
        this.tx.enqueue(this.reactive::decodeAsScoredValue);
        return this.reactive._zpopmax(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zpopmax(K key, int count) {
        this.tx.enqueue(this.reactive::decodeAsListOfScoredValues);
        return this.reactive._zpopmax(key, count).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zpopmin(K key) {
        this.tx.enqueue(this.reactive::decodeAsScoredValue);
        return this.reactive._zpopmin(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zpopmin(K key, int count) {
        this.tx.enqueue(this.reactive::decodeAsListOfScoredValues);
        return this.reactive._zpopmin(key, count).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zrandmember(K key) {
        this.tx.enqueue(this.reactive::decodeV);
        return this.reactive._zrandmember(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zrandmember(K key, int count) {
        this.tx.enqueue(this.reactive::decodeAsListOfValues);
        return this.reactive._zrandmember(key, count).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zrandmemberWithScores(K key) {
        this.tx.enqueue(this.reactive::decodeAsScoredValueOrEmpty);
        return this.reactive._zrandmemberWithScores(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zrandmemberWithScores(K key, int count) {
        this.tx.enqueue(this.reactive::decodeAsListOfScoredValues);
        return this.reactive._zrandmemberWithScores(key, count).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> bzpopmin(Duration timeout, K... keys) {
        this.tx.enqueue(this.reactive::decodeAsKeyValue);
        return this.reactive._bzpopmin(timeout, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> bzpopmax(Duration timeout, K... keys) {
        this.tx.enqueue(this.reactive::decodeAsKeyValue);
        return this.reactive._bzpopmax(timeout, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zrange(K key, long start, long stop, ZRangeArgs args) {
        this.tx.enqueue(this.reactive::decodeAsListOfValues);
        return this.reactive._zrange(key, start, stop, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zrangeWithScores(K key, long start, long stop, ZRangeArgs args) {
        this.tx.enqueue(this.reactive::decodeAsListOfScoredValues);
        return this.reactive._zrangeWithScores(key, start, stop, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zrange(K key, long start, long stop) {
        this.tx.enqueue(this.reactive::decodeAsListOfValues);
        return this.reactive._zrange(key, start, stop).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zrangeWithScores(K key, long start, long stop) {
        this.tx.enqueue(this.reactive::decodeAsListOfScoredValues);
        return this.reactive._zrangeWithScores(key, start, stop).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zrangebylex(K key, Range<String> range, ZRangeArgs args) {
        this.tx.enqueue(this.reactive::decodeAsListOfValues);
        return this.reactive._zrangebylex(key, range, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zrangebylex(K key, Range<String> range) {
        this.tx.enqueue(this.reactive::decodeAsListOfValues);
        return this.reactive._zrangebylex(key, range).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zrangebyscore(K key, ScoreRange<Double> range, ZRangeArgs args) {
        this.tx.enqueue(this.reactive::decodeAsListOfValues);
        return this.reactive._zrangebyscore(key, range, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zrangebyscoreWithScores(K key, ScoreRange<Double> range, ZRangeArgs args) {
        this.tx.enqueue(this.reactive::decodeAsListOfScoredValues);
        return this.reactive._zrangebyscoreWithScores(key, range, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zrangebyscore(K key, ScoreRange<Double> range) {
        this.tx.enqueue(this.reactive::decodeAsListOfValues);
        return this.reactive._zrangebyscore(key, range).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zrangebyscoreWithScores(K key, ScoreRange<Double> range) {
        this.tx.enqueue(this.reactive::decodeAsListOfScoredValues);
        return this.reactive._zrangebyscoreWithScores(key, range).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zrangestore(K dst, K src, long min, long max, ZRangeArgs args) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._zrangestore(dst, src, min, max, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zrangestore(K dst, K src, long min, long max) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._zrangestore(dst, src, min, max).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zrangestorebylex(K dst, K src, Range<String> range, ZRangeArgs args) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._zrangestorebylex(dst, src, range, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zrangestorebylex(K dst, K src, Range<String> range) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._zrangestorebylex(dst, src, range).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zrangestorebyscore(K dst, K src, ScoreRange<Double> range, ZRangeArgs args) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._zrangestorebyscore(dst, src, range, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zrangestorebyscore(K dst, K src, ScoreRange<Double> range) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._zrangestorebyscore(dst, src, range).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zrank(K key, V member) {
        this.tx.enqueue(this.reactive::decodeAsLong);
        return this.reactive._zrank(key, member).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zrem(K key, V... members) {
        this.tx.enqueue(Response::toInteger);
        return this.reactive._zrem(key, members).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zremrangebylex(K key, Range<String> range) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._zremrangebylex(key, range).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zremrangebyrank(K key, long start, long stop) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._zremrangebyrank(key, start, stop).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zremrangebyscore(K key, ScoreRange<Double> range) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._zremrangebyscore(key, range).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zrevrank(K key, V member) {
        this.tx.enqueue(this.reactive::decodeAsLong);
        return this.reactive._zrevrank(key, member).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zscore(K key, V member) {
        this.tx.enqueue(this.reactive::decodeAsDouble);
        return this.reactive._zscore(key, member).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zunion(ZAggregateArgs args, K... keys) {
        this.tx.enqueue(this.reactive::decodeAsListOfValues);
        return this.reactive._zunion(args, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zunion(K... keys) {
        this.tx.enqueue(this.reactive::decodeAsListOfValues);
        return this.reactive._zunion(keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zunionWithScores(ZAggregateArgs args, K... keys) {
        this.tx.enqueue(this.reactive::decodeAsListOfScoredValues);
        return this.reactive._zunionWithScores(args, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zunionWithScores(K... keys) {
        this.tx.enqueue(this.reactive::decodeAsListOfScoredValues);
        return this.reactive._zunionWithScores(keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zunionstore(K destination, ZAggregateArgs args, K... keys) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._zunionstore(destination, args, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> zunionstore(K destination, K... keys) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._zunionstore(destination, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

}
