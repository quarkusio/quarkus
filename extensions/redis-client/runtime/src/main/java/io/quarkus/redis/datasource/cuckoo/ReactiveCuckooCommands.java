package io.quarkus.redis.datasource.cuckoo;

import java.util.List;

import io.quarkus.redis.datasource.ReactiveRedisCommands;
import io.smallrye.mutiny.Uni;

/**
 * Allows executing commands from the {@code cuckoo} group. These commands require the
 * <a href="https://redis.io/docs/stack/bloom/">Redis Bloom</a> module (this modules also include Cuckoo filters) to be
 * installed in the Redis server.
 * <p>
 * See <a href="https://redis.io/commands/?group=cf">the cuckoo command list</a> for further information about these
 * commands.
 * <p>
 *
 * @param <K>
 *        the type of the key
 * @param <V>
 *        the type of the value stored in the filter
 */
public interface ReactiveCuckooCommands<K, V> extends ReactiveRedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/cf.add">CF.ADD</a>. Summary: Adds the specified element to
     * the specified Cuckoo filter. Group: cuckoo
     * <p>
     * If the cuckoo filter does not exist, it creates a new one.
     *
     * @param key
     *        the key
     * @param value
     *        the value, must not be {@code null}
     *
     * @return a uni producing {@code null} when the operation completes
     **/
    Uni<Void> cfadd(K key, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/cf.addnx">CF.ADDNX</a>. Summary: Adds an item to a cuckoo
     * filter if the item did not exist previously. Group: cuckoo
     * <p>
     * If the cuckoo filter does not exist, it creates a new one.
     *
     * @param key
     *        the key
     * @param value
     *        the value, must not be {@code null}
     *
     * @return a Uni producing {@code true} if the value was added to the filter, {@code false} otherwise
     **/
    Uni<Boolean> cfaddnx(K key, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/cf.count">CF.COUNT</a>. Summary: Returns the number of
     * times an item may be in the filter. Because this is a probabilistic data structure, this may not necessarily be
     * accurate. Group: cuckoo
     * <p>
     *
     * @param key
     *        the key
     * @param value
     *        the value, must not be {@code null}
     *
     * @return a Uni producing the count of possible matching copies of the value in the filter
     **/
    Uni<Long> cfcount(K key, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/cf.del">CF.DEL</a>. Summary: Deletes an item once from the
     * filter. If the item exists only once, it will be removed from the filter. If the item was added multiple times,
     * it will still be present. Group: cuckoo
     * <p>
     *
     * @param key
     *        the key
     * @param value
     *        the value, must not be {@code null}
     *
     * @return a Uni producing {@code true} if the value was removed from the filter, {@code false} otherwise (the value
     *         was not found in the filter)
     **/
    Uni<Boolean> cfdel(K key, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/cf.exists">CF.EXISTS</a>. Summary: Check if an item exists
     * in a Cuckoo filter Group: cuckoo
     * <p>
     *
     * @param key
     *        the key
     * @param value
     *        the value, must not be {@code null}
     *
     * @return a Uni producing {@code true} if the value was found in the filter, {@code false} otherwise.
     **/
    Uni<Boolean> cfexists(K key, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/cf.insert">CF.INSERT</a>. Summary: Adds one or more items
     * to a cuckoo filter, allowing the filter to be created with a custom capacity if it does not exist yet. Group:
     * cuckoo
     * <p>
     *
     * @param key
     *        the key
     * @param values
     *        the values, must not be {@code null}, must not be empty, must not contain {@code null}
     *
     * @return a uni producing {@code null} when the operation completes
     **/
    Uni<Void> cfinsert(K key, V... values);

    /**
     * Execute the command <a href="https://redis.io/commands/cf.insert">CF.INSERT</a>. Summary: Adds one or more items
     * to a cuckoo filter, allowing the filter to be created with a custom capacity if it does not exist yet. Group:
     * cuckoo
     * <p>
     *
     * @param key
     *        the key
     * @param args
     *        the extra arguments
     * @param values
     *        the values, must not be {@code null}, must not be empty, must not contain {@code null}
     *
     * @return a uni producing {@code null} when the operation completes
     **/
    Uni<Void> cfinsert(K key, CfInsertArgs args, V... values);

    /**
     * Execute the command <a href="https://redis.io/commands/cf.insertnx">CF.INSERTNX</a>. Summary: Adds one or more
     * items to a cuckoo filter, allowing the filter to be created with a custom capacity if it does not exist yet.
     * Group: cuckoo
     * <p>
     *
     * @param key
     *        the key
     * @param values
     *        the values, must not be {@code null}, must not be empty, must not contain {@code null}
     *
     * @return a uni producing a list of boolean. For each added value, the corresponding boolean is {@code true} if the
     *         value has been added (non-existing) or {@code false} if the value was already present in the filter.
     **/
    Uni<List<Boolean>> cfinsertnx(K key, V... values);

    /**
     * Execute the command <a href="https://redis.io/commands/cf.insertnx">CF.INSERTNX</a>. Summary: Adds one or more
     * items to a cuckoo filter, allowing the filter to be created with a custom capacity if it does not exist yet.
     * Group: cuckoo
     * <p>
     *
     * @param key
     *        the key
     * @param args
     *        the extra arguments
     * @param values
     *        the values, must not be {@code null}, must not be empty, must not contain {@code null}
     *
     * @return a uni producing a list of boolean. For each added value, the corresponding boolean is {@code true} if the
     *         value has been added (non-existing) or {@code false} if the value was already present in the filter.
     **/
    Uni<List<Boolean>> cfinsertnx(K key, CfInsertArgs args, V... values);

    /**
     * Execute the command <a href="https://redis.io/commands/cf.mexists">CF.MEXISTS</a>. Summary: Check if an item
     * exists in a Cuckoo filter Group: cuckoo
     * <p>
     *
     * @param key
     *        the key
     * @param values
     *        the values, must not be {@code null}, must not contain {@code null}, must not be empty
     *
     * @return a uni producing a list of boolean indicating, for each corresponding value, if the value exists in the
     *         filter or not.
     **/
    Uni<List<Boolean>> cfmexists(K key, V... values);

    /**
     * Execute the command <a href="https://redis.io/commands/cf.reserve">CF.RESERVE</a>. Summary: Create a Cuckoo
     * Filter as key with a single sub-filter for the initial amount of capacity for items. Group: cuckoo
     * <p>
     *
     * @param key
     *        the key
     * @param capacity
     *        the capacity
     *
     * @return a uni producing {@code null} when the operation completes
     **/
    Uni<Void> cfreserve(K key, long capacity);

    /**
     * Execute the command <a href="https://redis.io/commands/cf.reserve">CF.RESERVE</a>. Summary: Create a Cuckoo
     * Filter as key with a single sub-filter for the initial amount of capacity for items. Group: cuckoo
     * <p>
     *
     * @param key
     *        the key
     * @param capacity
     *        the capacity
     * @param args
     *        the extra parameters
     *
     * @return a uni producing {@code null} when the operation completes
     **/
    Uni<Void> cfreserve(K key, long capacity, CfReserveArgs args);

}
