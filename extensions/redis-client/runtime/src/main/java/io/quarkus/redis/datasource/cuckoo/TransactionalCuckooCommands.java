package io.quarkus.redis.datasource.cuckoo;

import io.quarkus.redis.datasource.TransactionalRedisCommands;

/**
 * Allows executing commands from the {@code cuckoo} group. These commands require the
 * <a href="https://redis.io/docs/stack/bloom/">Redis Bloom</a> module (this modules also include Cuckoo filters) to be
 * installed in the Redis server.
 * <p>
 * See <a href="https://redis.io/commands/?group=cf">the cuckoo command list</a> for further information about these
 * commands.
 * <p>
 * This API is intended to be used in a Redis transaction ({@code MULTI}), thus, all command methods return
 * {@code void}.
 *
 * @param <K>
 *        the type of the key
 * @param <V>
 *        the type of the value stored in the filter
 */
public interface TransactionalCuckooCommands<K, V> extends TransactionalRedisCommands {

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
     */
    void cfadd(K key, V value);

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
     */
    void cfaddnx(K key, V value);

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
     */
    void cfcount(K key, V value);

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
     */
    void cfdel(K key, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/cf.exists">CF.EXISTS</a>. Summary: Check if an item exists
     * in a Cuckoo filter Group: cuckoo
     * <p>
     *
     * @param key
     *        the key
     * @param value
     *        the value, must not be {@code null}
     */
    void cfexists(K key, V value);

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
     */
    void cfinsert(K key, V... values);

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
     */
    void cfinsert(K key, CfInsertArgs args, V... values);

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
     */
    void cfinsertnx(K key, V... values);

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
     */
    void cfinsertnx(K key, CfInsertArgs args, V... values);

    /**
     * Execute the command <a href="https://redis.io/commands/cf.mexists">CF.MEXISTS</a>. Summary: Check if an item
     * exists in a Cuckoo filter Group: cuckoo
     * <p>
     *
     * @param key
     *        the key
     * @param values
     *        the values, must not be {@code null}, must not contain {@code null}, must not be empty
     */
    void cfmexists(K key, V... values);

    /**
     * Execute the command <a href="https://redis.io/commands/cf.reserve">CF.RESERVE</a>. Summary: Create a Cuckoo
     * Filter as key with a single sub-filter for the initial amount of capacity for items. Group: cuckoo
     * <p>
     *
     * @param key
     *        the key
     * @param capacity
     *        the capacity
     */
    void cfreserve(K key, long capacity);

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
     */
    void cfreserve(K key, long capacity, CfReserveArgs args);
}
