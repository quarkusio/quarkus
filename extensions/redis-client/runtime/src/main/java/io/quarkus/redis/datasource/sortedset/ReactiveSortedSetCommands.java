package io.quarkus.redis.datasource.sortedset;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.ReactiveRedisCommands;
import io.quarkus.redis.datasource.ScanArgs;
import io.quarkus.redis.datasource.SortArgs;
import io.quarkus.redis.datasource.list.KeyValue;
import io.smallrye.mutiny.Uni;

/**
 * Allows executing commands from the {@code sorted set} group. See
 * <a href="https://redis.io/commands/?group=sorted-set">the sorted set command list</a> for further information about
 * these commands.
 * <p>
 * A {@code sorted set} is a set of value associated with scores. These scores allow comparing the elements. As a
 * result, we obtain a sorted set.
 * <p>
 * Scores are of type {@code double}.
 *
 * @param <K>
 *        the type of the key
 * @param <V>
 *        the type of the scored item
 */
public interface ReactiveSortedSetCommands<K, V> extends ReactiveRedisCommands {

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
     * @return {@code true} if the element was added, {@code false} otherwise
     **/
    Uni<Boolean> zadd(K key, double score, V member);

    /**
     * Execute the command <a href="https://redis.io/commands/zadd">ZADD</a>. Summary: Add one or more members to a
     * sorted set, or update its score if it already exists Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param items
     *        the map of value/score to be added
     *
     * @return the number of added elements
     **/
    Uni<Integer> zadd(K key, Map<V, Double> items);

    /**
     * Execute the command <a href="https://redis.io/commands/zadd">ZADD</a>. Summary: Add one or more members to a
     * sorted set, or update its score if it already exists Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param items
     *        the pairs of score/value to be added
     *
     * @return the number of added elements
     **/
    Uni<Integer> zadd(K key, ScoredValue<V>... items);

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
     * @return {@code true} if the element was added or changed, {@code false} otherwise
     **/
    Uni<Boolean> zadd(K key, ZAddArgs zAddArgs, double score, V member);

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
     * @return the number of items added to the set
     **/
    Uni<Integer> zadd(K key, ZAddArgs zAddArgs, Map<V, Double> items);

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
     * @return the number of items added to the set
     **/
    Uni<Integer> zadd(K key, ZAddArgs zAddArgs, ScoredValue<V>... items);

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
     * @return the new score of the updated member, or {@code null} if the operation was aborted.
     */
    Uni<Double> zaddincr(K key, double score, V member);

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
     * @return the new score of the updated member, or {@code null} if the operation was aborted (when called with
     *         either the XX or the NX option).
     */
    Uni<Double> zaddincr(K key, ZAddArgs zAddArgs, double score, V member);

    /**
     * Execute the command <a href="https://redis.io/commands/zcard">ZCARD</a>. Summary: Get the number of members in a
     * sorted set Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     *
     * @return the cardinality (number of elements) of the sorted set. {@code 0} if the key does not exist.
     **/
    Uni<Long> zcard(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/zcount">ZCOUNT</a>. Summary: Count the members in a sorted
     * set with scores within the given values Group: sorted-set Requires Redis 2.0.0
     *
     * @param key
     *        the key
     * @param range
     *        the range
     *
     * @return the number of elements in the specified score range.
     **/
    Uni<Long> zcount(K key, ScoreRange<Double> range);

    /**
     * Execute the command <a href="https://redis.io/commands/zdiff">ZDIFF</a>. Summary: Subtract multiple sorted sets
     * Group: sorted-set Requires Redis 6.2.0
     *
     * @param keys
     *        the keys
     *
     * @return the result of the difference.
     **/
    Uni<List<V>> zdiff(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zdiff">ZDIFF</a>. Summary: Subtract multiple sorted sets,
     * and returns the list of keys with their scores Group: sorted-set Requires Redis 6.2.0
     *
     * @param keys
     *        the keys
     *
     * @return the result of the difference.
     **/
    Uni<List<ScoredValue<V>>> zdiffWithScores(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zdiffstore">ZDIFFSTORE</a>. Summary: Subtract multiple
     * sorted sets and store the resulting sorted set in a new key Group: sorted-set Requires Redis 6.2.0
     *
     * @param destination
     *        the destination key
     * @param keys
     *        the keys to compare
     *
     * @return the number of elements in the resulting sorted set at destination.
     **/
    Uni<Long> zdiffstore(K destination, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zincrby">ZINCRBY</a>. Summary: Increment the score of a
     * member in a sorted set Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     *
     * @return the new score of member.
     **/
    Uni<Double> zincrby(K key, double increment, V member);

    /**
     * Execute the command <a href="https://redis.io/commands/zinter">ZINTER</a>. Summary: Intersect multiple sorted
     * sets Group: sorted-set Requires Redis 6.2.0
     *
     * @param arguments
     *        the ZINTER command extra-arguments
     * @param keys
     *        the keys
     *
     * @return the result of intersection.
     **/
    Uni<List<V>> zinter(ZAggregateArgs arguments, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zinter">ZINTER</a>. Summary: Intersect multiple sorted
     * sets Group: sorted-set Requires Redis 6.2.0
     *
     * @param keys
     *        the keys
     *
     * @return the result of intersection.
     **/
    Uni<List<V>> zinter(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zinter">ZINTER</a>. Summary: Intersect multiple sorted
     * sets Group: sorted-set Requires Redis 6.2.0
     *
     * @param arguments
     *        the ZINTER command extra-arguments
     * @param keys
     *        the keys
     *
     * @return the result of intersection with the scores
     **/
    Uni<List<ScoredValue<V>>> zinterWithScores(ZAggregateArgs arguments, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zinter">ZINTER</a>. Summary: Intersect multiple sorted
     * sets Group: sorted-set Requires Redis 6.2.0
     *
     * @param keys
     *        the keys
     *
     * @return the result of intersection with the scores
     **/
    Uni<List<ScoredValue<V>>> zinterWithScores(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zintercard">ZINTERCARD</a>. Summary: Intersect multiple
     * sorted sets and return the cardinality of the result Group: sorted-set Requires Redis 7.0.0
     *
     * @param keys
     *        the keys
     *
     * @return the number of elements in the resulting intersection.
     **/
    Uni<Long> zintercard(K... keys);

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
     * @return the number of elements in the resulting intersection.
     **/
    Uni<Long> zintercard(long limit, K... keys);

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
     * @return the number of elements in the resulting sorted set at destination.
     **/
    Uni<Long> zinterstore(K destination, ZAggregateArgs arguments, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zinterstore">ZINTERSTORE</a>. Summary: Intersect multiple
     * sorted sets and store the resulting sorted set in a new key Group: sorted-set Requires Redis 2.0.0
     *
     * @param destination
     *        the destination key
     * @param keys
     *        the keys of the sorted set to analyze
     *
     * @return the number of elements in the resulting sorted set at destination.
     **/
    Uni<Long> zinterstore(K destination, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zlexcount">ZLEXCOUNT</a>. Summary: Count the number of
     * members in a sorted set between a given lexicographical range Group: sorted-set Requires Redis 2.8.9
     *
     * @param key
     *        the key
     * @param range
     *        the range
     *
     * @return the number of elements in the specified score range.
     **/
    Uni<Long> zlexcount(K key, Range<String> range);

    /**
     * Execute the command <a href="https://redis.io/commands/zmpop">ZMPOP</a>. Summary: Remove and return members with
     * scores in a sorted set Group: sorted-set Requires Redis 7.0.0
     * <p>
     * The elements popped are those with the lowest scores from the first non-empty sorted set.
     *
     * @param keys
     *        the keys
     *
     * @return The popped element (value / score), or {@code null} if no element can be popped.
     **/
    Uni<ScoredValue<V>> zmpopMin(K... keys);

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
     * @return The popped element (value / score), or {@code empty} if no element can be popped.
     **/
    Uni<List<ScoredValue<V>>> zmpopMin(int count, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zmpop">ZMPOP</a>. Summary: Remove and return members with
     * scores in a sorted set Group: sorted-set Requires Redis 7.0.0
     * <p>
     * The elements with the highest scores to be popped.
     *
     * @param keys
     *        the keys
     *
     * @return The popped element (value / score), or {@code null} if no element can be popped.
     **/
    Uni<ScoredValue<V>> zmpopMax(K... keys);

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
     * @return The popped element (value / score), or {@code empty} if no element can be popped.
     **/
    Uni<List<ScoredValue<V>>> zmpopMax(int count, K... keys);

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
     * @return The popped element (value / score), or {@code null} if no element can be popped.
     **/
    Uni<ScoredValue<V>> bzmpopMin(Duration timeout, K... keys);

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
     * @return The popped element (value / score), or {@code empty} if no element can be popped.
     **/
    Uni<List<ScoredValue<V>>> bzmpopMin(Duration timeout, int count, K... keys);

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
     * @return The popped element (value / score), or {@code null} if no element can be popped.
     **/
    Uni<ScoredValue<V>> bzmpopMax(Duration timeout, K... keys);

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
     * @return The popped element (value / score), or {@code empty} if no element can be popped.
     **/
    Uni<List<ScoredValue<V>>> bzmpopMax(Duration timeout, int count, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zmscore">ZMSCORE</a>. Summary: Get the score associated
     * with the given members in a sorted set Group: sorted-set Requires Redis 6.2.0
     *
     * @param key
     *        the key
     * @param members
     *        the members
     *
     * @return list of scores or {@code null} associated with the specified member values.
     **/
    Uni<List<Double>> zmscore(K key, V... members);

    /**
     * Execute the command <a href="https://redis.io/commands/zpopmax">ZPOPMAX</a>. Summary: Remove and return members
     * with the highest scores in a sorted set Group: sorted-set Requires Redis 5.0.0
     *
     * @param key
     *        the key
     *
     * @return the popped element and score, {@link ScoredValue#EMPTY} is not found.
     **/
    Uni<ScoredValue<V>> zpopmax(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/zpopmax">ZPOPMAX</a>. Summary: Remove and return members
     * with the highest scores in a sorted set Group: sorted-set Requires Redis 5.0.0
     *
     * @param key
     *        the key
     *
     * @return the popped elements and scores.
     **/
    Uni<List<ScoredValue<V>>> zpopmax(K key, int count);

    /**
     * Execute the command <a href="https://redis.io/commands/zpopmin">ZPOPMIN</a>. Summary: Remove and return members
     * with the lowest scores in a sorted set Group: sorted-set Requires Redis 5.0.0
     *
     * @param key
     *        the key
     *
     * @return the popped element and score.
     **/
    Uni<ScoredValue<V>> zpopmin(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/zpopmin">ZPOPMIN</a>. Summary: Remove and return members
     * with the lowest scores in a sorted set Group: sorted-set Requires Redis 5.0.0
     *
     * @param key
     *        the key
     *
     * @return the popped elements and scores.
     **/
    Uni<List<ScoredValue<V>>> zpopmin(K key, int count);

    /**
     * Execute the command <a href="https://redis.io/commands/zrandmember">ZRANDMEMBER</a>. Summary: Get one or multiple
     * random elements from a sorted set Group: sorted-set Requires Redis 6.2.0
     *
     * @param key
     *        the key
     *
     * @return the randomly selected element, or {@code null} when key does not exist.
     **/
    Uni<V> zrandmember(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/zrandmember">ZRANDMEMBER</a>. Summary: Get one or multiple
     * random elements from a sorted set Group: sorted-set Requires Redis 6.2.0
     *
     * @param key
     *        the key
     * @param count
     *        the number of member to select
     *
     * @return the list of elements, or an empty array when key does not exist.
     **/
    Uni<List<V>> zrandmember(K key, int count);

    /**
     * Execute the command <a href="https://redis.io/commands/zrandmember">ZRANDMEMBER</a>. Summary: Get one or multiple
     * random elements from a sorted set Group: sorted-set Requires Redis 6.2.0
     *
     * @param key
     *        the key
     *
     * @return the randomly selected element and its score, or {@code null} when key does not exist.
     **/
    Uni<ScoredValue<V>> zrandmemberWithScores(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/zrandmember">ZRANDMEMBER</a>. Summary: Get one or multiple
     * random elements from a sorted set Group: sorted-set Requires Redis 6.2.0
     *
     * @param key
     *        the key
     * @param count
     *        the number of member to select
     *
     * @return the list of elements (with their score), or an empty array when key does not exist.
     **/
    Uni<List<ScoredValue<V>>> zrandmemberWithScores(K key, int count);

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
     * @return {@code null} when no element could be popped and the timeout expired. A structure containing the key and
     *         the {@link ScoredValue} with the popped value and the score.
     **/
    Uni<KeyValue<K, ScoredValue<V>>> bzpopmin(Duration timeout, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/bzpopmax">BZPOPMAX</a>. Summary: Remove and return the
     * member with the highest score from one or more sorted sets, or block until one is available Group: sorted-set
     * Requires Redis 5.0.0
     *
     * @return {@code null} when no element could be popped and the timeout expired. A structure containing the key and
     *         the {@link ScoredValue} with the popped value and the score.
     **/
    Uni<KeyValue<K, ScoredValue<V>>> bzpopmax(Duration timeout, K... keys);

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
     * @return list of elements in the specified range.
     **/
    Uni<List<V>> zrange(K key, long start, long stop, ZRangeArgs args);

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
     * @return list of elements with their scores in the specified range.
     **/
    Uni<List<ScoredValue<V>>> zrangeWithScores(K key, long start, long stop, ZRangeArgs args);

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
     * @return list of elements in the specified range.
     **/
    Uni<List<V>> zrange(K key, long start, long stop);

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
     * @return list of elements with their scores in the specified range.
     **/
    Uni<List<ScoredValue<V>>> zrangeWithScores(K key, long start, long stop);

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
     * @return list of elements in the specified range.
     **/
    Uni<List<V>> zrangebylex(K key, Range<String> range, ZRangeArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/zrange">ZRANGE</a>. Summary: Return a range of members in
     * a sorted set using lexicographical ranges Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param range
     *        the range
     *
     * @return list of elements in the specified range.
     **/
    Uni<List<V>> zrangebylex(K key, Range<String> range);

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
     * @return list of elements in the specified range.
     **/
    Uni<List<V>> zrangebyscore(K key, ScoreRange<Double> range, ZRangeArgs args);

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
     * @return list of elements with their scores in the specified range.
     **/
    Uni<List<ScoredValue<V>>> zrangebyscoreWithScores(K key, ScoreRange<Double> range, ZRangeArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/zrange">ZRANGE</a>. Summary: Return a range of members in
     * a sorted set using score ranges Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param range
     *        the range
     *
     * @return list of elements in the specified range.
     **/
    Uni<List<V>> zrangebyscore(K key, ScoreRange<Double> range);

    /**
     * Execute the command <a href="https://redis.io/commands/zrange">ZRANGE</a>. Summary: Return a range of members in
     * a sorted set using score ranges Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param range
     *        the range
     *
     * @return list of elements with their scores in the specified range.
     **/
    Uni<List<ScoredValue<V>>> zrangebyscoreWithScores(K key, ScoreRange<Double> range);

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
     * @return the number of elements in the resulting sorted set.
     **/
    Uni<Long> zrangestore(K dst, K src, long min, long max, ZRangeArgs args);

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
     * @return the number of elements in the resulting sorted set.
     **/
    Uni<Long> zrangestore(K dst, K src, long min, long max);

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
     * @return the number of elements in the resulting sorted set.
     **/
    Uni<Long> zrangestorebylex(K dst, K src, Range<String> range, ZRangeArgs args);

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
     * @return the number of elements in the resulting sorted set.
     **/
    Uni<Long> zrangestorebylex(K dst, K src, Range<String> range);

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
     * @return the number of elements in the resulting sorted set.
     **/
    Uni<Long> zrangestorebyscore(K dst, K src, ScoreRange<Double> range, ZRangeArgs args);

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
     * @return the number of elements in the resulting sorted set.
     **/
    Uni<Long> zrangestorebyscore(K dst, K src, ScoreRange<Double> range);

    /**
     * Execute the command <a href="https://redis.io/commands/zrank">ZRANK</a>. Summary: Determine the index of a member
     * in a sorted set Group: sorted-set Requires Redis 2.0.0
     *
     * @param key
     *        the key
     *
     * @return the rank of member. If member does not exist in the sorted set or key does not exist, {@code null}.
     **/
    Uni<Long> zrank(K key, V member);

    /**
     * Execute the command <a href="https://redis.io/commands/zrem">ZREM</a>. Summary: Remove one or more members from a
     * sorted set Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param members
     *        the members to remove
     *
     * @return The number of members removed from the sorted set, not including non-existing members.
     **/
    Uni<Integer> zrem(K key, V... members);

    /**
     * Execute the command <a href="https://redis.io/commands/zremrangebylex">ZREMRANGEBYLEX</a>. Summary: Remove all
     * members in a sorted set between the given lexicographical range Group: sorted-set Requires Redis 2.8.9
     *
     * @param key
     *        the key
     * @param range
     *        the range
     *
     * @return the number of elements removed.
     **/
    Uni<Long> zremrangebylex(K key, Range<String> range);

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
     * @return the number of elements removed.
     **/
    Uni<Long> zremrangebyrank(K key, long start, long stop);

    /**
     * Execute the command <a href="https://redis.io/commands/zremrangebyscore">ZREMRANGEBYSCORE</a>. Summary: Remove
     * all members in a sorted set within the given scores Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param range
     *        the range
     *
     * @return the number of elements removed.
     **/
    Uni<Long> zremrangebyscore(K key, ScoreRange<Double> range);

    /**
     * Execute the command <a href="https://redis.io/commands/zrevrank">ZREVRANK</a>. Summary: Determine the index of a
     * member in a sorted set, with scores ordered from high to low Group: sorted-set Requires Redis 2.0.0
     *
     * @param key
     *        the key
     *
     * @return the rank of member. If member does not exist in the sorted set or key does not exist, {@code null}.
     **/
    Uni<Long> zrevrank(K key, V member);

    /**
     * Execute the command <a href="https://redis.io/commands/zscan">ZSCAN</a>. Summary: Incrementally iterate sorted
     * sets elements and associated scores Group: sorted-set Requires Redis 2.8.0
     *
     * @param key
     *        the key
     *
     * @return the cursor to iterate over the sorted set
     **/
    ReactiveZScanCursor<V> zscan(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/zscan">ZSCAN</a>. Summary: Incrementally iterate sorted
     * sets elements and associated scores Group: sorted-set Requires Redis 2.8.0
     *
     * @param key
     *        the key
     * @param args
     *        the extra scan arguments
     *
     * @return the cursor to iterate over the sorted set
     **/
    ReactiveZScanCursor<V> zscan(K key, ScanArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/zscore">ZSCORE</a>. Summary: Get the score associated with
     * the given member in a sorted set Group: sorted-set Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param member
     *        the member
     *
     * @return the score of member, {@code null} if the member cannot be found or the key does not exist
     **/
    Uni<Double> zscore(K key, V member);

    /**
     * Execute the command <a href="https://redis.io/commands/zunion">ZUNION</a>. Summary: Add multiple sorted sets
     * Group: sorted-set Requires Redis 6.2.0
     *
     * @param args
     *        the ZUNION command extra-arguments
     * @param keys
     *        the keys
     *
     * @return the result of union
     **/
    Uni<List<V>> zunion(ZAggregateArgs args, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zunion">ZUNION</a>. Summary: Add multiple sorted sets
     * Group: sorted-set Requires Redis 6.2.0
     *
     * @param keys
     *        the keys
     *
     * @return the result of union
     **/
    Uni<List<V>> zunion(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zunion">ZUNION</a>. Summary: Add multiple sorted sets
     * Group: sorted-set Requires Redis 6.2.0
     *
     * @param keys
     *        the keys
     *
     * @return the result of union
     **/
    Uni<List<ScoredValue<V>>> zunionWithScores(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zunion">ZUNION</a>. Summary: Add multiple sorted sets
     * Group: sorted-set Requires Redis 6.2.0
     *
     * @param args
     *        the ZUNION command extra-arguments
     * @param keys
     *        the keys
     *
     * @return the result of union
     **/
    Uni<List<ScoredValue<V>>> zunionWithScores(ZAggregateArgs args, K... keys);

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
     * @return the number of elements in the resulting sorted set at destination.
     **/
    Uni<Long> zunionstore(K destination, ZAggregateArgs args, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/zunionstore">ZUNIONSTORE</a>. Summary: Add multiple sorted
     * sets and store the resulting sorted set in a new key Group: sorted-set Requires Redis 2.0.0
     *
     * @param destination
     *        the destination key
     * @param keys
     *        the keys
     *
     * @return the number of elements in the resulting sorted set at destination.
     **/
    Uni<Long> zunionstore(K destination, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/sort">SORT</a>. Summary: Sort the elements in a list, set
     * or sorted set Group: generic Requires Redis 1.0.0
     *
     * @return the list of sorted elements.
     **/
    Uni<List<V>> sort(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/sort">SORT</a>. Summary: Sort the elements in a list, set
     * or sorted set Group: generic Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param sortArguments
     *        the {@code SORT} command extra-arguments
     *
     * @return the list of sorted elements.
     **/
    Uni<List<V>> sort(K key, SortArgs sortArguments);

    /**
     * Execute the command <a href="https://redis.io/commands/sort">SORT</a> with the {@code STORE} option. Summary:
     * Sort the elements in a list, set or sorted set Group: generic Requires Redis 1.0.0
     *
     * @param sortArguments
     *        the SORT command extra-arguments
     *
     * @return the number of sorted elements in the destination list.
     **/
    Uni<Long> sortAndStore(K key, K destination, SortArgs sortArguments);

    /**
     * Execute the command <a href="https://redis.io/commands/sort">SORT</a> with the {@code STORE} option. Summary:
     * Sort the elements in a list, set or sorted set Group: generic Requires Redis 1.0.0
     *
     * @return the number of sorted elements in the destination list.
     **/
    Uni<Long> sortAndStore(K key, K destination);

}
