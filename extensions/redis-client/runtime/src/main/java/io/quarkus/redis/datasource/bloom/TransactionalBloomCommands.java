package io.quarkus.redis.datasource.bloom;

import io.quarkus.redis.datasource.TransactionalRedisCommands;

public interface TransactionalBloomCommands<K, V> extends TransactionalRedisCommands {

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
     **/
    void bfadd(K key, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/bf.exists">BF.EXISTS</a>. Summary: Determines whether an
     * item may exist in the Bloom Filter or not. Group: bloom
     *
     * @param key
     *        the key
     * @param value
     *        the value, must not be {@code null}
     **/
    void bfexists(K key, V value);

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
     **/
    void bfmadd(K key, V... values);

    /**
     * Execute the command <a href="https://redis.io/commands/bf.mexists">BF.MEXISTS</a>. Summary: Determines if one or
     * more items may exist in the filter or not. Group: bloom
     *
     * @param key
     *        the key
     * @param values
     *        the values, must not be {@code null}, must not contain {@code null}, must not be empty.
     **/
    void bfmexists(K key, V... values);

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
     **/
    void bfinsert(K key, BfInsertArgs args, V... values);
}
