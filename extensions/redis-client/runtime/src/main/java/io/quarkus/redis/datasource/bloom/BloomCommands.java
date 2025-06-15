package io.quarkus.redis.datasource.bloom;

import java.util.List;

import io.quarkus.redis.datasource.RedisCommands;

/**
 * Allows executing commands from the {@code bloom} group. These commands require the
 * <a href="https://redis.io/docs/stack/bloom/">Redis Bloom</a> module to be installed in the Redis server.
 * <p>
 * See <a href="https://redis.io/commands/?group=bf">the bloom command list</a> for further information about these
 * commands.
 * <p>
 *
 * @param <K>
 *        the type of the key
 * @param <V>
 *        the type of the value stored in the filter
 */
public interface BloomCommands<K, V> extends RedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/bf.add">BF.ADD</a>. Summary: Adds the specified element to
     * the specified Bloom filter. Group: bloom
     * <p>
     * If the bloom filter does not exist, it creates a new one.
     *
     * @param key
     *        the key
     * @param value
     *        the value, must not be {@code null}
     *
     * @return {@code true} if the value did not exist in the filter, {@code false} otherwise.
     **/
    boolean bfadd(K key, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/bf.exists">BF.EXISTS</a>. Summary: Determines whether an
     * item may exist in the Bloom Filter or not. Group: bloom
     *
     * @param key
     *        the key
     * @param value
     *        the value, must not be {@code null}
     *
     * @return {@code true} if the value may exist in the filter, {@code false} means it does not exist in the filter.
     **/
    boolean bfexists(K key, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/bf.madd">BF.MADD</a>. Summary: Adds one or more items to
     * the Bloom Filter and creates the filter if it does not exist yet. This command operates identically to BF.ADD
     * except that it allows multiple inputs and returns multiple values. Group: bloom
     * <p>
     *
     * @param key
     *        the key
     * @param values
     *        the values, must not be {@code null}, must not contain {@code null}, must not be empty.
     *
     * @return a list of booleans. {@code true} if the corresponding value did not exist in the filter, {@code false}
     *         otherwise.
     **/
    List<Boolean> bfmadd(K key, V... values);

    /**
     * Execute the command <a href="https://redis.io/commands/bf.mexists">BF.MEXISTS</a>. Summary: Determines if one or
     * more items may exist in the filter or not. Group: bloom
     *
     * @param key
     *        the key
     * @param values
     *        the values, must not be {@code null}, must not contain {@code null}, must not be empty.
     *
     * @return a list of booleans. {@code true} if the corresponding value may exist in the filter, {@code false} does
     *         not exist in the filter.
     **/
    List<Boolean> bfmexists(K key, V... values);

    /**
     * Execute the command <a href="https://redis.io/commands/bf.reserve">BF.RESERVE</a>. Summary: Creates an empty
     * Bloom Filter with a single sub-filter for the initial capacity requested and with an upper bound
     * {@code error_rate}. Group: bloom
     *
     * @param key
     *        the key
     * @param errorRate
     *        The desired probability for false positives. The rate is a decimal value between 0 and 1.
     * @param capacity
     *        The number of entries intended to be added to the filter.
     **/
    void bfreserve(K key, double errorRate, long capacity);

    /**
     * Execute the command <a href="https://redis.io/commands/bf.reserve">BF.RESERVE</a>. Summary: Creates an empty
     * Bloom Filter with a single sub-filter for the initial capacity requested and with an upper bound
     * {@code error_rate}. Group: bloom
     *
     * @param key
     *        the key
     * @param errorRate
     *        The desired probability for false positives. The rate is a decimal value between 0 and 1.
     * @param capacity
     *        The number of entries intended to be added to the filter.
     * @param args
     *        the extra parameters
     **/
    void bfreserve(K key, double errorRate, long capacity, BfReserveArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/bf.insert">BF.INSERT</a>. Summary: BF.INSERT is a
     * sugarcoated combination of {@code BF.RESERVE} and {@code BF.ADD}. It creates a new filter if the key does not
     * exist using the relevant arguments. Next, all {@code ITEMS} are inserted. Group: bloom
     * <p>
     *
     * @param key
     *        the key
     * @param args
     *        the creation parameters
     * @param values
     *        the value to add, must not contain {@code null}, must not be {@code null}, must not be empty
     *
     * @return a list of booleans. {@code true} if the corresponding value may exist in the filter, {@code false} does
     *         not exist in the filter.
     **/
    List<Boolean> bfinsert(K key, BfInsertArgs args, V... values);

}
