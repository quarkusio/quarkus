package io.quarkus.redis.datasource.string;

import java.util.Map;

import io.quarkus.redis.datasource.TransactionalRedisCommands;

@Deprecated
public interface TransactionalStringCommands<K, V> extends TransactionalRedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/append">APPEND</a>. Summary: Append a value to a key
     * Group: string Requires Redis 2.0.0
     *
     * @param key
     *        the key
     * @param value
     *        the value
     */
    void append(K key, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/decr">DECR</a>. Summary: Decrement the integer value of a
     * key by one Group: string Requires Redis 1.0.0
     *
     * @param key
     *        the key
     */
    void decr(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/decrby">DECRBY</a>. Summary: Decrement the integer value
     * of a key by the given number Group: string Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param amount
     *        the amount, can be negative
     */
    void decrby(K key, long amount);

    /**
     * Execute the command <a href="https://redis.io/commands/get">GET</a>. Summary: Get the value of a key Group:
     * string Requires Redis 1.0.0
     *
     * @param key
     *        the key
     */
    void get(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/getdel">GETDEL</a>. Summary: Get the value of a key and
     * delete the key Group: string Requires Redis 6.2.0
     *
     * @param key
     *        the key
     */
    void getdel(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/getex">GETEX</a>. Summary: Get the value of a key and
     * optionally set its expiration Group: string Requires Redis 6.2.0
     *
     * @param key
     *        the key
     * @param args
     *        the getex command extra-arguments
     */
    void getex(K key, GetExArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/getrange">GETRANGE</a>. Summary: Get a substring of the
     * string stored at a key Group: string Requires Redis 2.4.0
     *
     * @param key
     *        the key
     * @param start
     *        the start offset
     * @param end
     *        the end offset
     */
    void getrange(K key, long start, long end);

    /**
     * Execute the command <a href="https://redis.io/commands/getset">GETSET</a>. Summary: Set the string value of a key
     * and return its old value Group: string Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param value
     *        the value
     *
     * @deprecated See https://redis.io/commands/getset
     */
    void getset(K key, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/incr">INCR</a>. Summary: Increment the integer value of a
     * key by one Group: string Requires Redis 1.0.0
     *
     * @param key
     *        the key
     */
    void incr(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/incrby">INCRBY</a>. Summary: Increment the integer value
     * of a key by the given amount Group: string Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param amount
     *        the amount, can be negative
     */
    void incrby(K key, long amount);

    /**
     * Execute the command <a href="https://redis.io/commands/incrbyfloat">INCRBYFLOAT</a>. Summary: Increment the float
     * value of a key by the given amount Group: string Requires Redis 2.6.0
     *
     * @param key
     *        the key
     * @param amount
     *        the amount, can be negative
     */
    void incrbyfloat(K key, double amount);

    /**
     * Execute the command <a href="https://redis.io/commands/lcs">LCS</a>. Summary: Find longest common substring
     * Group: string Requires Redis 7.0.0
     *
     * @param key1
     *        the key
     * @param key2
     *        the key
     */
    void lcs(K key1, K key2);

    /**
     * Execute the command <a href="https://redis.io/commands/lcs">LCS</a>. Summary: Find longest common substring and
     * return the length (using {@code LEN}) Group: string Requires Redis 7.0.0
     *
     * @param key1
     *        the key
     * @param key2
     *        the key
     */
    void lcsLength(K key1, K key2);

    /**
     * Execute the command <a href="https://redis.io/commands/mget">MGET</a>. Summary: Get the values of all the given
     * keys Group: string Requires Redis 1.0.0
     *
     * @param keys
     *        the keys
     */
    void mget(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/mset">MSET</a>. Summary: Set multiple keys to multiple
     * values Group: string Requires Redis 1.0.1
     *
     * @param map
     *        the key/value map containing the items to store
     */
    void mset(Map<K, V> map);

    /**
     * Execute the command <a href="https://redis.io/commands/msetnx">MSETNX</a>. Summary: Set multiple keys to multiple
     * values, only if none of the keys exist Group: string Requires Redis 1.0.1
     *
     * @param map
     *        the key/value map containing the items to store
     */
    void msetnx(Map<K, V> map);

    /**
     * Execute the command <a href="https://redis.io/commands/psetex">PSETEX</a>. Summary: Set the value and expiration
     * in milliseconds of a key Group: string Requires Redis 2.6.0
     *
     * @param key
     *        the key
     * @param milliseconds
     *        the duration in ms
     * @param value
     *        the value
     */
    void psetex(K key, long milliseconds, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/set">SET</a>. Summary: Set the string value of a key
     * Group: string Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param value
     *        the value
     */
    void set(K key, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/set">SET</a>. Summary: Set the string value of a key
     * Group: string Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param value
     *        the value
     * @param setArgs
     *        the set command extra-arguments
     */
    void set(K key, V value, SetArgs setArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/set">SET</a>. Summary: Set the string value of a key, and
     * return the previous value Group: string Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param value
     *        the value
     */
    void setGet(K key, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/set">SET</a>. Summary: Set the string value of a key, and
     * return the previous value Group: string Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param value
     *        the value
     * @param setArgs
     *        the set command extra-arguments
     */
    void setGet(K key, V value, SetArgs setArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/setex">SETEX</a>. Summary: Set the value and expiration of
     * a key Group: string Requires Redis 2.0.0
     *
     * @param key
     *        the key
     * @param value
     *        the value
     */
    void setex(K key, long seconds, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/setnx">SETNX</a>. Summary: Set the value of a key, only if
     * the key does not exist Group: string Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param value
     *        the value
     */
    void setnx(K key, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/setrange">SETRANGE</a>. Summary: Overwrite part of a
     * string at key starting at the specified offset Group: string Requires Redis 2.2.0
     *
     * @param key
     *        the key
     * @param value
     *        the value
     */
    void setrange(K key, long offset, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/strlen">STRLEN</a>. Summary: Get the length of the value
     * stored in a key Group: string Requires Redis 2.2.0
     *
     * @param key
     *        the key
     */
    void strlen(K key);
}
