package io.quarkus.redis.datasource.string;

import java.util.Map;

import io.quarkus.redis.datasource.RedisCommands;

/**
 * Allows executing commands from the {@code string} group. See <a href="https://redis.io/commands/?group=string">the
 * string command list</a> for further information about these commands.
 * <p>
 * This group can be used with value of type {@code String}, or a type which will be automatically
 * serialized/deserialized with a codec.
 *
 * @param <K>
 *        the type of the key
 * @param <V>
 *        the type of the value
 *
 * @deprecated Use {@link io.quarkus.redis.datasource.value.ValueCommands} instead.
 */
@Deprecated
public interface StringCommands<K, V> extends RedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/append">APPEND</a>. Summary: Append a value to a key
     * Group: string Requires Redis 2.0.0
     *
     * @param key
     *        the key
     * @param value
     *        the value
     *
     * @return the length of the string after the append operation.
     **/
    long append(K key, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/decr">DECR</a>. Summary: Decrement the integer value of a
     * key by one Group: string Requires Redis 1.0.0
     *
     * @param key
     *        the key
     *
     * @return the value of key after the decrement
     **/
    long decr(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/decrby">DECRBY</a>. Summary: Decrement the integer value
     * of a key by the given number Group: string Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param amount
     *        the amount, can be negative
     *
     * @return the value of key after the decrement
     **/
    long decrby(K key, long amount);

    /**
     * Execute the command <a href="https://redis.io/commands/get">GET</a>. Summary: Get the value of a key Group:
     * string Requires Redis 1.0.0
     *
     * @param key
     *        the key
     *
     * @return the value of key, or {@code null} when key does not exist.
     **/
    V get(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/getdel">GETDEL</a>. Summary: Get the value of a key and
     * delete the key Group: string Requires Redis 6.2.0
     *
     * @param key
     *        the key
     *
     * @return the value of key, {@code null} when key does not exist, or an error if the key's value type isn't a
     *         string.
     **/
    V getdel(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/getex">GETEX</a>. Summary: Get the value of a key and
     * optionally set its expiration Group: string Requires Redis 6.2.0
     *
     * @param key
     *        the key
     * @param args
     *        the getex command extra-arguments
     *
     * @return the value of key, or {@code null} when key does not exist.
     **/
    V getex(K key, GetExArgs args);

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
     *
     * @return the sub-string
     **/
    String getrange(K key, long start, long end);

    /**
     * Execute the command <a href="https://redis.io/commands/getset">GETSET</a>. Summary: Set the string value of a key
     * and return its old value Group: string Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param value
     *        the value
     *
     * @return the old value stored at key, or {@code null} when key did not exist.
     *
     * @deprecated See https://redis.io/commands/getset
     **/
    V getset(K key, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/incr">INCR</a>. Summary: Increment the integer value of a
     * key by one Group: string Requires Redis 1.0.0
     *
     * @param key
     *        the key
     *
     * @return the value of key after the increment
     **/
    long incr(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/incrby">INCRBY</a>. Summary: Increment the integer value
     * of a key by the given amount Group: string Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param amount
     *        the amount, can be negative
     *
     * @return the value of key after the increment
     **/
    long incrby(K key, long amount);

    /**
     * Execute the command <a href="https://redis.io/commands/incrbyfloat">INCRBYFLOAT</a>. Summary: Increment the float
     * value of a key by the given amount Group: string Requires Redis 2.6.0
     *
     * @param key
     *        the key
     * @param amount
     *        the amount, can be negative
     *
     * @return the value of key after the increment.
     **/
    double incrbyfloat(K key, double amount);

    /**
     * Execute the command <a href="https://redis.io/commands/lcs">LCS</a>. Summary: Find longest common substring
     * Group: string Requires Redis 7.0.0
     *
     * @param key1
     *        the key
     * @param key2
     *        the key
     *
     * @return the string representing the longest common substring is returned.
     **/
    String lcs(K key1, K key2);

    /**
     * Execute the command <a href="https://redis.io/commands/lcs">LCS</a>. Summary: Find longest common substring and
     * return the length (using {@code LEN}) Group: string Requires Redis 7.0.0
     *
     * @param key1
     *        the key
     * @param key2
     *        the key
     *
     * @return the length of the longest common substring.
     **/
    long lcsLength(K key1, K key2);

    // TODO Add LCS with IDX support

    /**
     * Execute the command <a href="https://redis.io/commands/mget">MGET</a>. Summary: Get the values of all the given
     * keys Group: string Requires Redis 1.0.0
     *
     * @param keys
     *        the keys
     *
     * @return list of values at the specified keys. If one of the passed key does not exist, the returned map contains
     *         a {@code null} value associated with the missing key.
     **/
    Map<K, V> mget(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/mset">MSET</a>. Summary: Set multiple keys to multiple
     * values Group: string Requires Redis 1.0.1
     *
     * @param map
     *        the key/value map containing the items to store
     *
     * @return a Uni producing a {@code null} item on success, a failure otherwise
     **/
    void mset(Map<K, V> map);

    /**
     * Execute the command <a href="https://redis.io/commands/msetnx">MSETNX</a>. Summary: Set multiple keys to multiple
     * values, only if none of the keys exist Group: string Requires Redis 1.0.1
     *
     * @param map
     *        the key/value map containing the items to store
     *
     * @return {@code true} the all the keys were set. {@code false} no key was set (at least one key already existed).
     **/
    boolean msetnx(Map<K, V> map);

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
     *
     * @return a Uni producing a {@code null} item on success, a failure otherwise
     **/
    void psetex(K key, long milliseconds, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/set">SET</a>. Summary: Set the string value of a key
     * Group: string Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param value
     *        the value
     *
     * @return a Uni producing a {@code null} item on success, a failure otherwise
     **/
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
     *
     * @return a Uni producing a {@code null} item on success, a failure otherwise
     **/
    void set(K key, V value, SetArgs setArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/set">SET</a>. Summary: Set the string value of a key, and
     * return the previous value Group: string Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param value
     *        the value
     *
     * @return the old value, {@code null} if not present
     **/
    V setGet(K key, V value);

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
     *
     * @return the old value, {@code null} if not present
     **/
    V setGet(K key, V value, SetArgs setArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/setex">SETEX</a>. Summary: Set the value and expiration of
     * a key Group: string Requires Redis 2.0.0
     *
     * @param key
     *        the key
     * @param value
     *        the value
     **/
    void setex(K key, long seconds, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/setnx">SETNX</a>. Summary: Set the value of a key, only if
     * the key does not exist Group: string Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param value
     *        the value
     *
     * @return {@code true} the key was set {@code false} the key was not set
     **/
    boolean setnx(K key, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/setrange">SETRANGE</a>. Summary: Overwrite part of a
     * string at key starting at the specified offset Group: string Requires Redis 2.2.0
     *
     * @param key
     *        the key
     * @param value
     *        the value
     *
     * @return the length of the string after it was modified by the command.
     **/
    long setrange(K key, long offset, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/strlen">STRLEN</a>. Summary: Get the length of the value
     * stored in a key Group: string Requires Redis 2.2.0
     *
     * @param key
     *        the key
     *
     * @return the length of the string at key, or 0 when key does not exist.
     **/
    long strlen(K key);

}
