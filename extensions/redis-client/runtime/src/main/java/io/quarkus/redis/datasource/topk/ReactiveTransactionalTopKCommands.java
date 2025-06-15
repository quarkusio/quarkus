package io.quarkus.redis.datasource.topk;

import java.util.Map;

import io.quarkus.redis.datasource.ReactiveTransactionalRedisCommands;
import io.smallrye.mutiny.Uni;

/**
 * Allows executing commands from the {@code top-k} group. These commands require the
 * <a href="https://redis.io/docs/stack/bloom/">Redis Bloom</a> module (this modules also include Top-K list support) to
 * be installed in the Redis server.
 * <p>
 * See <a href="https://redis.io/commands/?name=topk.">the top-k command list</a> for further information about these
 * commands.
 * <p>
 * This API is intended to be used in a Redis transaction ({@code MULTI}), thus, all command methods return
 * {@code Uni<Void>}.
 *
 * @param <K>
 *        the type of the key
 * @param <V>
 *        the type of the value stored in the sketch
 */
public interface ReactiveTransactionalTopKCommands<K, V> extends ReactiveTransactionalRedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/topk.add">TOPK.ADD</a>. Summary: Adds an item to the data
     * structure. Multiple items can be added at once. If an item enters the Top-K list, the item which is expelled is
     * returned. This allows dynamic heavy-hitter detection of items being entered or expelled from Top-K list. Group:
     * top-k
     * <p>
     *
     * @param key
     *        the name of list where item is added, must not be {@code null}
     * @param item
     *        the item to add, must not be {@code null}
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> topkAdd(K key, V item);

    /**
     * Execute the command <a href="https://redis.io/commands/topk.add">TOPK.ADD</a>. Summary: Adds an item to the data
     * structure. Multiple items can be added at once. If an item enters the Top-K list, the item which is expelled is
     * returned. This allows dynamic heavy-hitter detection of items being entered or expelled from Top-K list. Group:
     * top-k
     * <p>
     *
     * @param key
     *        the name of list where item is added, must not be {@code null}
     * @param items
     *        the items to add, must not be {@code null}, must not be empty, must not contain {@code null}
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> topkAdd(K key, V... items);

    /**
     * Execute the command <a href="https://redis.io/commands/topk.incrby">TOPK.INCRBY</a>. Summary: Increase the score
     * of an item in the data structure by increment. Multiple items' score can be increased at once. If an item enters
     * the Top-K list, the item which is expelled is returned. Group: top-k
     * <p>
     *
     * @param key
     *        the name of list where item is added, must not be {@code null}
     * @param item
     *        the item to add, must not be {@code null}
     * @param increment
     *        increment to current item score. Increment must be greater or equal to 1. Increment is limited to
     *        100,000 to avoid server freeze.
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> topkIncrBy(K key, V item, int increment);

    /**
     * Execute the command <a href="https://redis.io/commands/topk.incrby">TOPK.INCRBY</a>. Summary: Increase the score
     * of an item in the data structure by increment. Multiple items' score can be increased at once. If an item enters
     * the Top-K list, the item which is expelled is returned. Group: top-k
     * <p>
     *
     * @param key
     *        the name of list where item is added, must not be {@code null}
     * @param couples
     *        The map containing the item / increment, must not be {@code null}, must not be empty
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> topkIncrBy(K key, Map<V, Integer> couples);

    /**
     * Execute the command <a href="https://redis.io/commands/topk.list/">TOPK.LIST</a>. Summary: Return full list of
     * items in Top K list. Group: top-k
     * <p>
     *
     * @param key
     *        the name of list, must not be {@code null}
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> topkList(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/topk.list/">TOPK.LIST</a>. Summary: Return full list of
     * items in Top K list. Group: top-k
     * <p>
     *
     * @param key
     *        the name of list, must not be {@code null}
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> topkListWithCount(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/topk.query/">TOPK.QUERY</a>. Summary: Checks whether an
     * item is one of Top-K items. Multiple items can be checked at once. Group: top-k
     * <p>
     *
     * @param key
     *        the name of list, must not be {@code null}
     * @param item
     *        the item to check, must not be {@code null}
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> topkQuery(K key, V item);

    /**
     * Execute the command <a href="https://redis.io/commands/topk.query/">TOPK.QUERY</a>. Summary: Checks whether an
     * item is one of Top-K items. Multiple items can be checked at once. Group: top-k
     * <p>
     *
     * @param key
     *        the name of list, must not be {@code null}
     * @param items
     *        the items to check, must not be {@code null}, must not contain {@code null}, must not be empty
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> topkQuery(K key, V... items);

    /**
     * Execute the command <a href="https://redis.io/commands/topk.reserve/">TOPK.RESERVE</a>. Summary: Initializes a
     * TopK with specified parameters. Group: top-k
     * <p>
     *
     * @param key
     *        the name of list, must not be {@code null}
     * @param topk
     *        the number of top occurring items to keep.
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> topkReserve(K key, int topk);

    /**
     * Execute the command <a href="https://redis.io/commands/topk.reserve/">TOPK.RESERVE</a>. Summary: Initializes a
     * TopK with specified parameters. Group: top-k
     * <p>
     *
     * @param key
     *        the name of list, must not be {@code null}
     * @param topk
     *        the number of top occurring items to keep.
     * @param width
     *        the number of counters kept in each array. (Default 8)
     * @param depth
     *        the number of arrays. (Default 7)
     * @param decay
     *        the probability of reducing a counter in an occupied bucket. It is raised to power of it's counter
     *        (decay ^ bucket[i].counter). Therefore, as the counter gets higher, the chance of a reduction is being
     *        reduced. (Default 0.9)
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> topkReserve(K key, int topk, int width, int depth, double decay);
}
