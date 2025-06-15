package io.quarkus.redis.datasource.topk;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.redis.datasource.RedisCommands;

/**
 * Allows executing commands from the {@code top-k} group. These commands require the
 * <a href="https://redis.io/docs/stack/bloom/">Redis Bloom</a> module (this modules also include Top-K list support) to
 * be installed in the Redis server.
 * <p>
 * See <a href="https://redis.io/commands/?name=topk.">the top-k command list</a> for further information about these
 * commands.
 * <p>
 *
 * @param <K>
 *        the type of the key
 * @param <V>
 *        the type of the value stored in the sketch
 */
public interface TopKCommands<K, V> extends RedisCommands {

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
     * @return an optional containing the item that get expelled if any, empty otherwise
     */
    Optional<V> topkAdd(K key, V item);

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
     * @return a list containing for each corresponding added item the expelled item if any, {@code null} otherwise.
     */
    List<V> topkAdd(K key, V... items);

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
     * @return an optional containing the item that get expelled if any, empty otherwise
     */
    Optional<V> topkIncrBy(K key, V item, int increment);

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
     * @return a map containing for each added item the expelled item if any, {@code null} otherwise
     */
    Map<V, V> topkIncrBy(K key, Map<V, Integer> couples);

    /**
     * Execute the command <a href="https://redis.io/commands/topk.list/">TOPK.LIST</a>. Summary: Return full list of
     * items in Top K list. Group: top-k
     * <p>
     *
     * @param key
     *        the name of list, must not be {@code null}
     *
     * @return the list of items
     */
    List<V> topkList(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/topk.list/">TOPK.LIST</a>. Summary: Return full list of
     * items in Top K list. Group: top-k
     * <p>
     *
     * @param key
     *        the name of list, must not be {@code null}
     *
     * @return the Map of items with the associated count
     */
    Map<V, Integer> topkListWithCount(K key);

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
     * @return {@code true} if the item is in the list, {@code false} otherwise
     */
    boolean topkQuery(K key, V item);

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
     * @return a list containing {@code true} if the corresponding item is in the list, {@code false} otherwise
     */
    List<Boolean> topkQuery(K key, V... items);

    /**
     * Execute the command <a href="https://redis.io/commands/topk.reserve/">TOPK.RESERVE</a>. Summary: Initializes a
     * TopK with specified parameters. Group: top-k
     * <p>
     *
     * @param key
     *        the name of list, must not be {@code null}
     * @param topk
     *        the number of top occurring items to keep.
     */
    void topkReserve(K key, int topk);

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
     */
    void topkReserve(K key, int topk, int width, int depth, double decay);
}
