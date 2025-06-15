package io.quarkus.redis.datasource.topk;

import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.ReactiveRedisCommands;
import io.smallrye.mutiny.Uni;

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
public interface ReactiveTopKCommands<K, V> extends ReactiveRedisCommands {

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
     * @return a uni producing the item that get expelled if any, emit {@code null} otherwise
     **/
    Uni<V> topkAdd(K key, V item);

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
     * @return a uni producing a list containing for each corresponding added item the expelled item if any,
     *         {@code null} otherwise.
     **/
    Uni<List<V>> topkAdd(K key, V... items);

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
     * @return a uni producing the item that get expelled if any, emit {@code null} otherwise
     **/
    Uni<V> topkIncrBy(K key, V item, int increment);

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
     * @return a uni producing a map containing for each added item the expelled item if any, {@code null} otherwise
     **/
    Uni<Map<V, V>> topkIncrBy(K key, Map<V, Integer> couples);

    /**
     * Execute the command <a href="https://redis.io/commands/topk.list/">TOPK.LIST</a>. Summary: Return full list of
     * items in Top K list. Group: top-k
     * <p>
     *
     * @param key
     *        the name of list, must not be {@code null}
     *
     * @return a uni producing the list of items
     **/
    Uni<List<V>> topkList(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/topk.list/">TOPK.LIST</a>. Summary: Return full list of
     * items in Top K list. Group: top-k
     * <p>
     *
     * @param key
     *        the name of list, must not be {@code null}
     *
     * @return a uni producing the Map of items with the associated count
     **/
    Uni<Map<V, Integer>> topkListWithCount(K key);

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
     * @return a uni producing {@code true} if the item is in the list, {@code false} otherwise
     **/
    Uni<Boolean> topkQuery(K key, V item);

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
     * @return a uni producing a list containing {@code true} if the corresponding item is in the list, {@code false}
     *         otherwise
     **/
    Uni<List<Boolean>> topkQuery(K key, V... items);

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
     * @return a uni producing {@code null} once the operation completes
     **/
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
     * @return a uni producing {@code null} once the operation completes
     **/
    Uni<Void> topkReserve(K key, int topk, int width, int depth, double decay);

}
