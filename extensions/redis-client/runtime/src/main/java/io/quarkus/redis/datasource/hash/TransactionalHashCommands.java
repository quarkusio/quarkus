package io.quarkus.redis.datasource.hash;

import java.util.Map;

import io.quarkus.redis.datasource.TransactionalRedisCommands;

public interface TransactionalHashCommands<K, F, V> extends TransactionalRedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/hdel">HDEL</a>. Summary: Delete one or more hash fields
     * Group: hash Requires Redis 2.0.0
     *
     * @param key
     *        the key
     */
    void hdel(K key, F... fields);

    /**
     * Execute the command <a href="https://redis.io/commands/hexists">HEXISTS</a>. Summary: Determine if a hash field
     * exists Group: hash Requires Redis 2.0.0
     *
     * @param key
     *        the key
     * @param field
     *        the value
     */
    void hexists(K key, F field);

    /**
     * Execute the command <a href="https://redis.io/commands/hget">HGET</a>. Summary: Get the value of a hash field
     * Group: hash Requires Redis 2.0.0
     *
     * @param key
     *        the key
     * @param field
     *        the value
     */
    void hget(K key, F field);

    /**
     * Execute the command <a href="https://redis.io/commands/hincrby">HINCRBY</a>. Summary: Increment the
     * <strong>integer</strong> value of a hash field by the given number Group: hash Requires Redis 2.0.0
     *
     * @param key
     *        the key
     * @param field
     *        the value
     */
    void hincrby(K key, F field, long amount);

    /**
     * Execute the command <a href="https://redis.io/commands/hincrbyfloat">HINCRBYFLOAT</a>. Summary: Increment the
     * <strong>float</strong> value of a hash field by the given amount Group: hash Requires Redis 2.6.0
     *
     * @param key
     *        the key
     * @param field
     *        the value
     */
    void hincrbyfloat(K key, F field, double amount);

    /**
     * Execute the command <a href="https://redis.io/commands/hgetall">HGETALL</a>. Summary: Get all the fields and
     * values in a hash Group: hash Requires Redis 2.0.0
     *
     * @param key
     *        the key
     */
    void hgetall(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/hkeys">HKEYS</a>. Summary: Get all the fields in a hash
     * Group: hash Requires Redis 2.0.0
     *
     * @param key
     *        the key
     */
    void hkeys(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/hlen">HLEN</a>. Summary: Get the number of fields in a
     * hash Group: hash Requires Redis 2.0.0
     *
     * @param key
     *        the key
     */
    void hlen(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/hmget">HMGET</a>. Summary: Get the values of all the given
     * hash fields Group: hash Requires Redis 2.0.0
     *
     * @param key
     *        the key
     * @param fields
     *        the fields
     */
    void hmget(K key, F... fields);

    /**
     * Execute the command <a href="https://redis.io/commands/hmset">HMSET</a>. Summary: Set multiple hash fields to
     * multiple values Group: hash Requires Redis 2.0.0
     *
     * @param key
     *        the key
     * @param map
     *        the key/value map to set
     *
     * @deprecated Use {@link #hset(Object, Map)} with multiple field-value pairs.
     */
    @Deprecated
    void hmset(K key, Map<F, V> map);

    /**
     * Execute the command <a href="https://redis.io/commands/hrandfield">HRANDFIELD</a>. Summary: Get one or multiple
     * random fields from a hash Group: hash Requires Redis 6.2.0
     *
     * @param key
     *        the key
     */
    void hrandfield(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/hrandfield">HRANDFIELD</a>. Summary: Get one or multiple
     * random fields from a hash Group: hash Requires Redis 6.2.0
     *
     * @param key
     *        the key
     * @param count
     *        the number of random key to retrieve. If {@code count} is positive, the selected keys are distinct. If
     *        {@code count} is negative, the produced list can contain duplicated keys.
     */
    void hrandfield(K key, long count);

    /**
     * Execute the command <a href="https://redis.io/commands/hrandfield">HRANDFIELD</a>. Summary: Get one or multiple
     * random fields and their associated values from a hash Group: hash Requires Redis 6.2.0
     *
     * @param key
     *        the key
     * @param count
     *        the number of random key to retrieve. If {@code count} is positive, the selected keys are distinct. If
     *        {@code count} is negative, the produced list can contain duplicated keys. These duplicates are not
     *        included in the produced {@code Map}.
     */
    void hrandfieldWithValues(K key, long count);

    /**
     * Execute the command <a href="https://redis.io/commands/hset">HSET</a>. Summary: Set the string value of a hash
     * field Group: hash Requires Redis 2.0.0
     *
     * @param key
     *        the key
     * @param field
     *        the field
     * @param value
     *        the value
     */
    void hset(K key, F field, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/hset">HSET</a>. Summary: Set the string value of a hash
     * field Group: hash Requires Redis 2.0.0
     *
     * @param key
     *        the key
     * @param map
     *        the set of key -> value to add to the hash
     */
    void hset(K key, Map<F, V> map);

    /**
     * Execute the command <a href="https://redis.io/commands/hsetnx">HSETNX</a>. Summary: Set the value of a hash
     * field, only if the field does not exist Group: hash Requires Redis 2.0.0
     *
     * @param key
     *        the key
     * @param field
     *        the value
     * @param value
     *        the value
     */
    void hsetnx(K key, F field, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/hstrlen">HSTRLEN</a>. Summary: Get the length of the value
     * of a hash field Group: hash Requires Redis 3.2.0
     *
     * @param key
     *        the key
     * @param field
     *        the value
     */
    void hstrlen(K key, F field);

    /**
     * Execute the command <a href="https://redis.io/commands/hvals">HVALS</a>. Summary: Get all the values in a hash
     * Group: hash Requires Redis 2.0.0
     *
     * @param key
     *        the key
     */
    void hvals(K key);
}
