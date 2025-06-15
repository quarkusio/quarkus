package io.quarkus.redis.datasource.sortedset;

import java.time.Duration;
import java.util.Map;

import io.quarkus.redis.datasource.ReactiveTransactionalRedisCommands;
import io.smallrye.mutiny.Uni;

public interface ReactiveTransactionalSortedSetCommands<K, V> extends ReactiveTransactionalRedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/zadd">ZADD</a>. Summary: Add one or more members to a
     * sorted set, or update its score if it already exists Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param score
     *        the score
     * @param member
     *        the member
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zadd(K key, double score, V member);

    /**
     * Execute the command <a href="https://redis.io/commands/zadd">ZADD</a>. Summary: Add one or more members to a
     * sorted set, or update its score if it already exists Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param items
     *        the map of score/value to be added
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zadd(K key, Map<V, Double> items);

    /**
     * Execute the command <a href="https://redis.io/commands/zadd">ZADD</a>. Summary: Add one or more members to a
     * sorted set, or update its score if it already exists Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param items
     *        the pairs of value/score to be added
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zadd(K key, ScoredValue<V>... items);

    /**
     * Execute the command <a href="https://redis.io/commands/zadd">ZADD</a>. Summary: Add one or more members to a
     * sorted set, or update its score if it already exists Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param zAddArgs
     *        the extra parameter
     * @param score
     *        the score
     * @param member
     *        the member
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zadd(K key, ZAddArgs zAddArgs, double score, V member);

    /**
     * Execute the command <a href="https://redis.io/commands/zadd">ZADD</a>. Summary: Add one or more members to a
     * sorted set, or update its score if it already exists Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param zAddArgs
     *        the extra parameter
     * @param items
     *        the map of value/score to be added
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zadd(K key, ZAddArgs zAddArgs, Map<V, Double> items);

    /**
     * Execute the command <a href="https://redis.io/commands/zadd">ZADD</a>. Summary: Add one or more members to a
     * sorted set, or update its score if it already exists Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param zAddArgs
     *        the extra parameter
     * @param items
     *        the pairs of score/value to be added
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zadd(K key, ZAddArgs zAddArgs, ScoredValue<V>... items);

    /**
     * Execute the command <a href="https://redis.io/commands/zadd">ZADD</a>. Summary: Add one or more members to a
     * sorted set, or update its score if it already exists applying the {@code INCR} option Group: sorted-set Requires
     * Redis 1.2.0
     * <p>
     * This variant of {@code ZADD} acts like {@code ZINCRBY}. Only one score-element pair can be specified in this
     * mode.
     *
     * @param key
     *        the key.
     * @param score
     *        the increment.
     * @param member
     *        the member.
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zaddincr(K key, double score, V member);

    /**
     * Execute the command <a href="https://redis.io/commands/zadd">ZADD</a>. Summary: Add one or more members to a
     * sorted set, or update its score if it already exists applying the {@code INCR} option Group: sorted-set Requires
     * Redis 1.2.0
     * <p>
     * This variant of {@code ZADD} acts like {@code ZINCRBY}. Only one score-element pair can be specified in this
     * mode.
     *
     * @param key
     *        the key.
     * @param score
     *        the increment.
     * @param member
     *        the member.
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zaddincr(K key, ZAddArgs zAddArgs, double score, V member);

    /**
     * Execute the command <a href="https://redis.io/commands/zcard">ZCARD</a>. Summary: Get the number of members in a
     * sorted set Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zcard(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/zcount">ZCOUNT</a>. Summary: Count the members in a sorted
     * set with scores within the given values Group: sorted-set Requires Redis 2.0.0
     *
     * @param key
     *        the key
     * @param range
     *        the range
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zcount(K key, ScoreRange<Double> range);

    /**
     * Execute the command <a href="https://redis.io/commands/zdiff">ZDIFF</a>. Summary: Subtract multiple sorted sets
     * Group: sorted-set Requires Redis 6.2.0
     *
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zdiff(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zdiff">ZDIFF</a>. Summary: Subtract multiple sorted sets,
     * and returns the list of keys with their scores Group: sorted-set Requires Redis 6.2.0
     *
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zdiffWithScores(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zdiffstore">ZDIFFSTORE</a>. Summary: Subtract multiple
     * sorted sets and store the resulting sorted set in a new key Group: sorted-set Requires Redis 6.2.0
     *
     * @param destination
     *        the destination key
     * @param keys
     *        the keys to compare
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zdiffstore(K destination, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zincrby">ZINCRBY</a>. Summary: Increment the score of a
     * member in a sorted set Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zincrby(K key, double increment, V member);

    /**
     * Execute the command <a href="https://redis.io/commands/zinter">ZINTER</a>. Summary: Intersect multiple sorted
     * sets Group: sorted-set Requires Redis 6.2.0
     *
     * @param arguments
     *        the ZINTER command extra-arguments
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zinter(ZAggregateArgs arguments, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zinter">ZINTER</a>. Summary: Intersect multiple sorted
     * sets Group: sorted-set Requires Redis 6.2.0
     *
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zinter(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zinter">ZINTER</a>. Summary: Intersect multiple sorted
     * sets Group: sorted-set Requires Redis 6.2.0
     *
     * @param arguments
     *        the ZINTER command extra-arguments
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zinterWithScores(ZAggregateArgs arguments, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zinter">ZINTER</a>. Summary: Intersect multiple sorted
     * sets Group: sorted-set Requires Redis 6.2.0
     *
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zinterWithScores(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zintercard">ZINTERCARD</a>. Summary: Intersect multiple
     * sorted sets and return the cardinality of the result Group: sorted-set Requires Redis 7.0.0
     *
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zintercard(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zintercard">ZINTERCARD</a>. Summary: Intersect multiple
     * sorted sets and return the cardinality of the result Group: sorted-set Requires Redis 7.0.0
     *
     * @param limit
     *        if the intersection cardinality reaches limit partway through the computation, the algorithm will exit
     *        and yield limit as the cardinality.
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zintercard(long limit, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zinterstore">ZINTERSTORE</a>. Summary: Intersect multiple
     * sorted sets and store the resulting sorted set in a new key Group: sorted-set Requires Redis 2.0.0
     *
     * @param destination
     *        the destination key
     * @param arguments
     *        the ZINTERSTORE command extra-arguments
     * @param keys
     *        the keys of the sorted set to analyze
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zinterstore(K destination, ZAggregateArgs arguments, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zinterstore">ZINTERSTORE</a>. Summary: Intersect multiple
     * sorted sets and store the resulting sorted set in a new key Group: sorted-set Requires Redis 2.0.0
     *
     * @param destination
     *        the destination key
     * @param keys
     *        the keys of the sorted set to analyze
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zinterstore(K destination, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zlexcount">ZLEXCOUNT</a>. Summary: Count the number of
     * members in a sorted set between a given lexicographical range Group: sorted-set Requires Redis 2.8.9
     *
     * @param key
     *        the key
     * @param range
     *        the range
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zlexcount(K key, Range<String> range);

    /**
     * Execute the command <a href="https://redis.io/commands/zmpop">ZMPOP</a>. Summary: Remove and return members with
     * scores in a sorted set Group: sorted-set Requires Redis 7.0.0
     * <p>
     * The elements popped are those with the lowest scores from the first non-empty sorted set.
     *
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zmpopMin(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zmpop">ZMPOP</a>. Summary: Remove and return members with
     * scores in a sorted set Group: sorted-set Requires Redis 7.0.0
     * <p>
     * The elements popped are those with the lowest scores from the first non-empty sorted set.
     *
     * @param count
     *        the max number of element to pop
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zmpopMin(int count, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zmpop">ZMPOP</a>. Summary: Remove and return members with
     * scores in a sorted set Group: sorted-set Requires Redis 7.0.0
     * <p>
     * The elements with the highest scores to be popped.
     *
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zmpopMax(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zmpop">ZMPOP</a>. Summary: Remove and return members with
     * scores in a sorted set Group: sorted-set Requires Redis 7.0.0
     * <p>
     * The elements with the highest scores to be popped.
     *
     * @param count
     *        the max number of element to pop
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zmpopMax(int count, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/bzmpop">BZMPOP</a>. Summary: Remove and return members
     * with scores in a sorted set or block until one is available. Group: sorted-set Requires Redis 7.0.0
     * <p>
     * The elements popped are those with the lowest scores from the first non-empty sorted set.
     *
     * @param timeout
     *        the timeout
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> bzmpopMin(Duration timeout, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/bzmpop">BZMPOP</a>. Summary: Remove and return members
     * with scores in a sorted set or block until one is available. Group: sorted-set Requires Redis 7.0.0
     * <p>
     * The elements popped are those with the lowest scores from the first non-empty sorted set.
     *
     * @param timeout
     *        the timeout
     * @param count
     *        the max number of element to pop
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> bzmpopMin(Duration timeout, int count, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/bzmpop">BZMPOP</a>. Summary: Remove and return members
     * with scores in a sorted set or block until one is available. Group: sorted-set Requires Redis 7.0.0
     * <p>
     * The elements with the highest scores to be popped.
     *
     * @param timeout
     *        the timeout
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> bzmpopMax(Duration timeout, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/bzmpop">BZMPOP</a>. Summary: Remove and return members
     * with scores in a sorted set or block until one is available. Group: sorted-set Requires Redis 7.0.0
     * <p>
     * The elements with the highest scores to be popped.
     *
     * @param timeout
     *        the timeout
     * @param count
     *        the max number of element to pop
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> bzmpopMax(Duration timeout, int count, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zmscore">ZMSCORE</a>. Summary: Get the score associated
     * with the given members in a sorted set Group: sorted-set Requires Redis 6.2.0
     *
     * @param key
     *        the key
     * @param members
     *        the members
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zmscore(K key, V... members);

    /**
     * Execute the command <a href="https://redis.io/commands/zpopmax">ZPOPMAX</a>. Summary: Remove and return members
     * with the highest scores in a sorted set Group: sorted-set Requires Redis 5.0.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zpopmax(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/zpopmax">ZPOPMAX</a>. Summary: Remove and return members
     * with the highest scores in a sorted set Group: sorted-set Requires Redis 5.0.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zpopmax(K key, int count);

    /**
     * Execute the command <a href="https://redis.io/commands/zpopmin">ZPOPMIN</a>. Summary: Remove and return members
     * with the lowest scores in a sorted set Group: sorted-set Requires Redis 5.0.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zpopmin(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/zpopmin">ZPOPMIN</a>. Summary: Remove and return members
     * with the lowest scores in a sorted set Group: sorted-set Requires Redis 5.0.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zpopmin(K key, int count);

    /**
     * Execute the command <a href="https://redis.io/commands/zrandmember">ZRANDMEMBER</a>. Summary: Get one or multiple
     * random elements from a sorted set Group: sorted-set Requires Redis 6.2.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zrandmember(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/zrandmember">ZRANDMEMBER</a>. Summary: Get one or multiple
     * random elements from a sorted set Group: sorted-set Requires Redis 6.2.0
     *
     * @param key
     *        the key
     * @param count
     *        the number of member to select
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zrandmember(K key, int count);

    /**
     * Execute the command <a href="https://redis.io/commands/zrandmember">ZRANDMEMBER</a>. Summary: Get one or multiple
     * random elements from a sorted set Group: sorted-set Requires Redis 6.2.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zrandmemberWithScores(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/zrandmember">ZRANDMEMBER</a>. Summary: Get one or multiple
     * random elements from a sorted set Group: sorted-set Requires Redis 6.2.0
     *
     * @param key
     *        the key
     * @param count
     *        the number of member to select
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zrandmemberWithScores(K key, int count);

    /**
     * Execute the command <a href="https://redis.io/commands/bzpopmin">BZPOPMIN</a>. Summary: Remove and return the
     * member with the lowest score from one or more sorted sets, or block until one is available Group: sorted-set
     * Requires Redis 5.0.0
     *
     * @param timeout
     *        the max timeout
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> bzpopmin(Duration timeout, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/bzpopmax">BZPOPMAX</a>. Summary: Remove and return the
     * member with the highest score from one or more sorted sets, or block until one is available Group: sorted-set
     * Requires Redis 5.0.0
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> bzpopmax(Duration timeout, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zrange">ZRANGE</a>. Summary: Return a range of members in
     * a sorted set Group: sorted-set Requires Redis 1.2.0
     * <p>
     * This method extracts a range my rank/index in the stream.
     *
     * @param key
     *        the key
     * @param start
     *        the start position
     * @param stop
     *        the stop position
     * @param args
     *        the extra ZRANGE parameters
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zrange(K key, long start, long stop, ZRangeArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/zrange">ZRANGE</a>. Summary: Return a range of members in
     * a sorted set Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param start
     *        the start position
     * @param stop
     *        the stop position
     * @param args
     *        the extra ZRANGE parameters
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zrangeWithScores(K key, long start, long stop, ZRangeArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/zrange">ZRANGE</a>. Summary: Return a range of members in
     * a sorted set Group: sorted-set Requires Redis 1.2.0
     * <p>
     * This method extracts a range my rank/index in the stream.
     *
     * @param key
     *        the key
     * @param start
     *        the start position
     * @param stop
     *        the stop position
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zrange(K key, long start, long stop);

    /**
     * Execute the command <a href="https://redis.io/commands/zrange">ZRANGE</a>. Summary: Return a range of members in
     * a sorted set Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param start
     *        the start position
     * @param stop
     *        the stop position
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zrangeWithScores(K key, long start, long stop);

    /**
     * Execute the command <a href="https://redis.io/commands/zrange">ZRANGE</a>. Summary: Return a range of members in
     * a sorted set using lexicographical ranges Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param range
     *        the range
     * @param args
     *        the extra ZRANGE parameters
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zrangebylex(K key, Range<String> range, ZRangeArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/zrange">ZRANGE</a>. Summary: Return a range of members in
     * a sorted set using lexicographical ranges Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param range
     *        the range
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zrangebylex(K key, Range<String> range);

    /**
     * Execute the command <a href="https://redis.io/commands/zrange">ZRANGE</a>. Summary: Return a range of members in
     * a sorted set using score ranges Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param range
     *        the range
     * @param args
     *        the extra ZRANGE parameters
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zrangebyscore(K key, ScoreRange<Double> range, ZRangeArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/zrange">ZRANGE</a>. Summary: Return a range of members in
     * a sorted set using lexicographical ranges Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param range
     *        the range
     * @param args
     *        the extra ZRANGE parameters
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zrangebyscoreWithScores(K key, ScoreRange<Double> range, ZRangeArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/zrange">ZRANGE</a>. Summary: Return a range of members in
     * a sorted set using score ranges Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param range
     *        the range
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zrangebyscore(K key, ScoreRange<Double> range);

    /**
     * Execute the command <a href="https://redis.io/commands/zrange">ZRANGE</a>. Summary: Return a range of members in
     * a sorted set using score ranges Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param range
     *        the range
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zrangebyscoreWithScores(K key, ScoreRange<Double> range);

    /**
     * Execute the command <a href="https://redis.io/commands/zrangestore">ZRANGESTORE</a>. Summary: Store a range (by
     * rank) of members from sorted set into another key Group: sorted-set Requires Redis 6.2.0
     *
     * @param dst
     *        the key
     * @param src
     *        the key
     * @param min
     *        the lower bound of the range
     * @param max
     *        the upper bound of the range
     * @param args
     *        the ZRANGESTORE command extra-arguments
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zrangestore(K dst, K src, long min, long max, ZRangeArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/zrangestore">ZRANGESTORE</a>. Summary: Store a range (by
     * rank) of members from sorted set into another key Group: sorted-set Requires Redis 6.2.0
     *
     * @param dst
     *        the key
     * @param src
     *        the key
     * @param min
     *        the lower bound of the range
     * @param max
     *        the upper bound of the range
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zrangestore(K dst, K src, long min, long max);

    /**
     * Execute the command <a href="https://redis.io/commands/zrangestore">ZRANGESTORE</a>. Summary: Store a range (by
     * lexicographical order) of members from sorted set into another key Group: sorted-set Requires Redis 6.2.0
     *
     * @param dst
     *        the key
     * @param src
     *        the key
     * @param range
     *        the range
     * @param args
     *        the ZRANGESTORE command extra-arguments
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zrangestorebylex(K dst, K src, Range<String> range, ZRangeArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/zrangestore">ZRANGESTORE</a>. Summary: Store a range (by
     * lexicographical order) of members from sorted set into another key Group: sorted-set Requires Redis 6.2.0
     *
     * @param dst
     *        the key
     * @param src
     *        the key
     * @param range
     *        the range
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zrangestorebylex(K dst, K src, Range<String> range);

    /**
     * Execute the command <a href="https://redis.io/commands/zrangestore">ZRANGESTORE</a>. Summary: Store a range (by
     * score order) of members from sorted set into another key Group: sorted-set Requires Redis 6.2.0
     *
     * @param dst
     *        the key
     * @param src
     *        the key
     * @param range
     *        the range
     * @param args
     *        the ZRANGESTORE command extra-arguments
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zrangestorebyscore(K dst, K src, ScoreRange<Double> range, ZRangeArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/zrangestore">ZRANGESTORE</a>. Summary: Store a range (by
     * score order) of members from sorted set into another key Group: sorted-set Requires Redis 6.2.0
     *
     * @param dst
     *        the key
     * @param src
     *        the key
     * @param range
     *        the range
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zrangestorebyscore(K dst, K src, ScoreRange<Double> range);

    /**
     * Execute the command <a href="https://redis.io/commands/zrank">ZRANK</a>. Summary: Determine the index of a member
     * in a sorted set Group: sorted-set Requires Redis 2.0.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zrank(K key, V member);

    /**
     * Execute the command <a href="https://redis.io/commands/zrem">ZREM</a>. Summary: Remove one or more members from a
     * sorted set Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param members
     *        the members to remove
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zrem(K key, V... members);

    /**
     * Execute the command <a href="https://redis.io/commands/zremrangebylex">ZREMRANGEBYLEX</a>. Summary: Remove all
     * members in a sorted set between the given lexicographical range Group: sorted-set Requires Redis 2.8.9
     *
     * @param key
     *        the key
     * @param range
     *        the range
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zremrangebylex(K key, Range<String> range);

    /**
     * Execute the command <a href="https://redis.io/commands/zremrangebyrank">ZREMRANGEBYRANK</a>. Summary: Remove all
     * members in a sorted set within the given indexes Group: sorted-set Requires Redis 2.0.0
     *
     * @param key
     *        the key
     * @param start
     *        the lower bound of the range
     * @param stop
     *        the upper bound of the range
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zremrangebyrank(K key, long start, long stop);

    /**
     * Execute the command <a href="https://redis.io/commands/zremrangebyscore">ZREMRANGEBYSCORE</a>. Summary: Remove
     * all members in a sorted set within the given scores Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param range
     *        the range
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zremrangebyscore(K key, ScoreRange<Double> range);

    /**
     * Execute the command <a href="https://redis.io/commands/zrevrank">ZREVRANK</a>. Summary: Determine the index of a
     * member in a sorted set, with scores ordered from high to low Group: sorted-set Requires Redis 2.0.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zrevrank(K key, V member);

    /**
     * Execute the command <a href="https://redis.io/commands/zscore">ZSCORE</a>. Summary: Get the score associated with
     * the given member in a sorted set Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param member
     *        the member
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zscore(K key, V member);

    /**
     * Execute the command <a href="https://redis.io/commands/zunion">ZUNION</a>. Summary: Add multiple sorted sets
     * Group: sorted-set Requires Redis 6.2.0
     *
     * @param args
     *        the ZUNION command extra-arguments
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zunion(ZAggregateArgs args, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zunion">ZUNION</a>. Summary: Add multiple sorted sets
     * Group: sorted-set Requires Redis 6.2.0
     *
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zunion(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zunion">ZUNION</a>. Summary: Add multiple sorted sets
     * Group: sorted-set Requires Redis 6.2.0
     *
     * @param args
     *        the ZUNION command extra-arguments
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zunionWithScores(ZAggregateArgs args, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zunion">ZUNION</a>. Summary: Add multiple sorted sets
     * Group: sorted-set Requires Redis 6.2.0
     *
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zunionWithScores(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zunionstore">ZUNIONSTORE</a>. Summary: Add multiple sorted
     * sets and store the resulting sorted set in a new key Group: sorted-set Requires Redis 2.0.0
     *
     * @param destination
     *        the destination key
     * @param args
     *        the zunionstore command extra-arguments
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zunionstore(K destination, ZAggregateArgs args, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zunionstore">ZUNIONSTORE</a>. Summary: Add multiple sorted
     * sets and store the resulting sorted set in a new key Group: sorted-set Requires Redis 2.0.0
     *
     * @param destination
     *        the destination key
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> zunionstore(K destination, K... keys);
}
