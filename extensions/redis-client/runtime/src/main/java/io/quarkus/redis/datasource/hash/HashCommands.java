package io.quarkus.redis.datasource.hash;

import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.RedisCommands;
import io.quarkus.redis.datasource.ScanArgs;

/**
 * Allows executing commands from the {@code hash} group.
 * See <a href="https://redis.io/commands/?group=hash">the hash command list</a> for further information about these commands.
 *
 * A {@code hash} is a like a {@code Map&lt;F, V&gt;}.
 * This group is parameterized by the type of the key {@code Map&lt;K&gt;}. This is the type of the key in which the hash is
 * stored.
 * {@code &lt;F&gt;} is the type of the key in the map (field). The stored value are of type {@code Map&lt;V&gt;}
 *
 * @param <K> the type of the key
 * @param <F> the type of the field
 * @param <V> the type of the value
 */
public interface HashCommands<K, F, V> extends RedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/hdel">HDEL</a>.
     * Summary: Delete one or more hash fields
     * Group: hash
     * Requires Redis 2.0.0
     *
     * @param key the key
     * @return the number of fields that were removed from the hash, not including specified but
     *         non-existing fields.
     **/
    int hdel(K key, F... fields);

    /**
     * Execute the command <a href="https://redis.io/commands/hexists">HEXISTS</a>.
     * Summary: Determine if a hash field exists
     * Group: hash
     * Requires Redis 2.0.0
     *
     * @param key the key
     * @param field the value
     * @return {@code true} the hash contains field. {@code false} the hash does not contain field, or the key
     *         does not exist.
     **/
    boolean hexists(K key, F field);

    /**
     * Execute the command <a href="https://redis.io/commands/hget">HGET</a>.
     * Summary: Get the value of a hash field
     * Group: hash
     * Requires Redis 2.0.0
     *
     * @param key the key
     * @param field the value
     * @return the value associated with {@code field}, or {@code null} when {@code field} is not present in
     *         the hash or the key does not exist.
     **/
    V hget(K key, F field);

    /**
     * Execute the command <a href="https://redis.io/commands/hincrby">HINCRBY</a>.
     * Summary: Increment the <strong>integer</strong> value of a hash field by the given number
     * Group: hash
     * Requires Redis 2.0.0
     *
     * @param key the key
     * @param field the value
     * @return the value at field after the increment operation.
     **/
    long hincrby(K key, F field, long amount);

    /**
     * Execute the command <a href="https://redis.io/commands/hincrbyfloat">HINCRBYFLOAT</a>.
     * Summary: Increment the <strong>float</strong> value of a hash field by the given amount
     * Group: hash
     * Requires Redis 2.6.0
     *
     * @param key the key
     * @param field the value
     * @return the value of field after the increment.
     **/
    double hincrbyfloat(K key, F field, double amount);

    /**
     * Execute the command <a href="https://redis.io/commands/hgetall">HGETALL</a>.
     * Summary: Get all the fields and values in a hash
     * Group: hash
     * Requires Redis 2.0.0
     *
     * @param key the key
     * @return the map fields -> values stored in the hash, or an empty map when {@code key} does not exist.
     **/
    Map<F, V> hgetall(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/hkeys">HKEYS</a>.
     * Summary: Get all the fields in a hash
     * Group: hash
     * Requires Redis 2.0.0
     *
     * @param key the key
     * @return list of fields in the hash, or an empty list when key does not exist.
     **/
    List<F> hkeys(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/hlen">HLEN</a>.
     * Summary: Get the number of fields in a hash
     * Group: hash
     * Requires Redis 2.0.0
     *
     * @param key the key
     * @return number of fields in the hash, or 0 when key does not exist.
     **/
    long hlen(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/hmget">HMGET</a>.
     * Summary: Get the values of all the given hash fields
     * Group: hash
     * Requires Redis 2.0.0
     *
     * @param key the key must not be {@code null}
     * @param fields the fields, must not be empty, must not contain {@code null} values
     * @return list of values associated with the given fields, in the same order as they are requested.
     *         If a requested field does not exist, the returned map contains a {@code null} value for that field.
     **/
    Map<F, V> hmget(K key, F... fields);

    /**
     * Execute the command <a href="https://redis.io/commands/hmset">HMSET</a>.
     * Summary: Set multiple hash fields to multiple values
     * Group: hash
     * Requires Redis 2.0.0
     *
     * @param key the key
     * @param map the key/value map to set
     * @deprecated Use {@link #hset(Object, Map)} with multiple field-value pairs.
     **/
    @Deprecated
    void hmset(K key, Map<F, V> map);

    /**
     * Execute the command <a href="https://redis.io/commands/hrandfield">HRANDFIELD</a>.
     * Summary: Get one or multiple random fields from a hash
     * Group: hash
     * Requires Redis 6.2.0
     *
     * @param key the key
     * @return a random key from the hash, {@code null} if the key does not exist
     **/
    F hrandfield(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/hrandfield">HRANDFIELD</a>.
     * Summary: Get one or multiple random fields from a hash
     * Group: hash
     * Requires Redis 6.2.0
     *
     * @param key the key
     * @param count the number of random key to retrieve. If {@code count} is positive, the selected keys are distinct.
     *        If {@code count} is negative, the produced list can contain duplicated keys.
     * @return the list of keys, empty if the key does not exist
     **/
    List<F> hrandfield(K key, long count);

    /**
     * Execute the command <a href="https://redis.io/commands/hrandfield">HRANDFIELD</a>.
     * Summary: Get one or multiple random fields and their associated values from a hash
     * Group: hash
     * Requires Redis 6.2.0
     *
     * @param key the key
     * @param count the number of random key to retrieve. If {@code count} is positive, the selected keys are distinct.
     *        If {@code count} is negative, the produced list can contain duplicated keys. These duplicates are
     *        not included in the produced {@code Map}.
     * @return the map containing the random keys and the associated values, {@code empty} if the key does not exist
     **/
    Map<F, V> hrandfieldWithValues(K key, long count);

    /**
     * Execute the command <a href="https://redis.io/commands/hscan">HSCAN</a>.
     * Summary: Incrementally iterate hash fields and associated values
     * Group: hash
     * Requires Redis 2.8.0
     *
     * @param key the key
     * @return the cursor.
     **/
    HashScanCursor<F, V> hscan(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/hscan">HSCAN</a>.
     * Summary: Incrementally iterate hash fields and associated values
     * Group: hash
     * Requires Redis 2.8.0
     *
     * @param key the key
     * @param scanArgs the additional argument
     * @return the cursor
     **/
    HashScanCursor<F, V> hscan(K key, ScanArgs scanArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/hset">HSET</a>.
     * Summary: Set the string value of a hash field
     * Group: hash
     * Requires Redis 2.0.0
     *
     * @param key the key
     * @param field the field
     * @param value the value
     * @return {@code true} if the value was set
     **/
    boolean hset(K key, F field, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/hset">HSET</a>.
     * Summary: Set the string value of a hash field
     * Group: hash
     * Requires Redis 2.0.0
     *
     * @param key the key
     * @param map the set of key -> value to add to the hash
     * @return the number of fields that were added.
     **/
    long hset(K key, Map<F, V> map);

    /**
     * Execute the command <a href="https://redis.io/commands/hsetnx">HSETNX</a>.
     * Summary: Set the value of a hash field, only if the field does not exist
     * Group: hash
     * Requires Redis 2.0.0
     *
     * @param key the key
     * @param field the value
     * @param value the value
     * @return {@code true} field is a new field in the hash and value was set. {@code false} field already exists in
     *         the hash and no operation was performed.
     **/
    boolean hsetnx(K key, F field, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/hstrlen">HSTRLEN</a>.
     * Summary: Get the length of the value of a hash field
     * Group: hash
     * Requires Redis 3.2.0
     *
     * @param key the key
     * @param field the value
     * @return the string length of the value associated with {@code field}, or zero when {@code field} is not present
     *         in the hash or key does not exist at all.
     **/
    long hstrlen(K key, F field);

    /**
     * Execute the command <a href="https://redis.io/commands/hvals">HVALS</a>.
     * Summary: Get all the values in a hash
     * Group: hash
     * Requires Redis 2.0.0
     *
     * @param key the key
     * @return list of values in the hash, or an empty list when key does not exist.
     **/
    List<V> hvals(K key);

}
