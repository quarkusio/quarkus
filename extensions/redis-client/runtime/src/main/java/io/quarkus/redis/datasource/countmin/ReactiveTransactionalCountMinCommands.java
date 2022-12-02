package io.quarkus.redis.datasource.countmin;

import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.ReactiveTransactionalRedisCommands;
import io.smallrye.mutiny.Uni;

/**
 * Allows executing commands from the {@code count-min} group.
 * These commands require the <a href="https://redis.io/docs/stack/bloom/">Redis Bloom</a> module (this modules also
 * include Count-Min Sketches) to be installed in the Redis server.
 * <p>
 * See <a href="https://redis.io/commands/?name=cms">the count-min command list</a> for further information about
 * these commands.
 * <p>
 * This API is intended to be used in a Redis transaction ({@code MULTI}), thus, all command methods return {@code Uni<Void>}.
 *
 * @param <K> the type of the key
 * @param <V> the type of the value stored in the sketch
 */
public interface ReactiveTransactionalCountMinCommands<K, V> extends ReactiveTransactionalRedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/cms.incrby">CMS.INCRBY</a>.
     * Summary: Increases the count of item by increment. Multiple items can be increased with one call.
     * Group: count-min
     * <p>
     *
     * @param key the name of the sketch, must not be {@code null}
     * @param value the value, must not be {@code null}
     * @param increment the increment
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> cmsIncrBy(K key, V value, long increment);

    /**
     * Execute the command <a href="https://redis.io/commands/cms.incrby">CMS.INCRBY</a>.
     * Summary: Increases the count of item by increment. Multiple items can be increased with one call.
     * Group: count-min
     * <p>
     *
     * @param key the name of the sketch, must not be {@code null}
     * @param couples the set of value/increment pair, must not be {@code null}, must not be empty
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> cmsIncrBy(K key, Map<V, Long> couples);

    /**
     * Execute the command <a href="https://redis.io/commands/cms.initbydim">CMS.INITBYDIM</a>.
     * Summary: Initializes a Count-Min Sketch to dimensions specified by user.
     * Group: count-min
     * <p>
     *
     * @param key the name of the sketch, must not be {@code null}
     * @param width the number of counters in each array. Reduces the error size.
     * @param depth the number of counter-arrays. Reduces the probability for an error of a certain size (percentage of total
     *        count).
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> cmsInitByDim(K key, long width, long depth);

    /**
     * Execute the command <a href="https://redis.io/commands/cms.initbyprob">CMS.INITBYPROB</a>.
     * Summary: Initializes a Count-Min Sketch to accommodate requested tolerances.
     * Group: count-min
     * <p>
     *
     * @param key the name of the sketch, must not be {@code null}
     * @param error estimate size of error. The error is a percent of total counted items (in decimal). This
     *        effects the width of the sketch.
     * @param probability the desired probability for inflated count. This should be a decimal value between 0 and 1.
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> cmsInitByProb(K key, double error, double probability);

    /**
     * Execute the command <a href="https://redis.io/commands/cms.query">CMS.QUERY</a>.
     * Summary: Returns the count for one or more items in a sketch.
     * Group: count-min
     * <p>
     *
     * @param key the name of the sketch, must not be {@code null}
     * @param item the item to check, must not be {@code null}
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> cmsQuery(K key, V item);

    /**
     * Execute the command <a href="https://redis.io/commands/cms.query">CMS.QUERY</a>.
     * Summary: Returns the count for one or more items in a sketch.
     * Group: count-min
     * <p>
     *
     * @param key the name of the sketch, must not be {@code null}
     * @param items the items to check, must not be {@code null}, empty or contain {@code null}
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> cmsQuery(K key, V... items);

    /**
     * Execute the command <a href="https://redis.io/commands/cms.merge/">CMS.MERGE</a>.
     * Summary: Merges several sketches into one sketch. All sketches must have identical width and depth. Weights can
     * be used to multiply certain sketches. Default weight is 1.
     * Group: count-min
     * <p>
     *
     * @param dest The name of destination sketch. Must be initialized, must not be {@code null}
     * @param src The names of source sketches to be merged. Must not be {@code null}, must not contain {@code null},
     *        must not be empty.
     * @param weight The multiple of each sketch. Default =1, can be empty, can be {@code null}
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a failure
     *         otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> cmsMerge(K dest, List<K> src, List<Integer> weight);
}
