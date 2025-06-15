package io.quarkus.redis.datasource.hash;

import java.util.Map;

import io.quarkus.redis.datasource.ReactiveTransactionalRedisCommands;
import io.smallrye.mutiny.Uni;

public interface ReactiveTransactionalHashCommands<K, F, V> extends ReactiveTransactionalRedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/hdel">HDEL</a>. Summary: Delete one or more hash fields
     * Group: hash Requires Redis 2.0.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> hdel(K key, F... fields);

    /**
     * Execute the command <a href="https://redis.io/commands/hexists">HEXISTS</a>. Summary: Determine if a hash field
     * exists Group: hash Requires Redis 2.0.0
     *
     * @param key
     *        the key
     * @param field
     *        the value
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> hexists(K key, F field);

    /**
     * Execute the command <a href="https://redis.io/commands/hget">HGET</a>. Summary: Get the value of a hash field
     * Group: hash Requires Redis 2.0.0
     *
     * @param key
     *        the key
     * @param field
     *        the value
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> hget(K key, F field);

    /**
     * Execute the command <a href="https://redis.io/commands/hincrby">HINCRBY</a>. Summary: Increment the
     * <strong>integer</strong> value of a hash field by the given number Group: hash Requires Redis 2.0.0
     *
     * @param key
     *        the key
     * @param field
     *        the value
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> hincrby(K key, F field, long amount);

    /**
     * Execute the command <a href="https://redis.io/commands/hincrbyfloat">HINCRBYFLOAT</a>. Summary: Increment the
     * <strong>float</strong> value of a hash field by the given amount Group: hash Requires Redis 2.6.0
     *
     * @param key
     *        the key
     * @param field
     *        the value
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> hincrbyfloat(K key, F field, double amount);

    /**
     * Execute the command <a href="https://redis.io/commands/hgetall">HGETALL</a>. Summary: Get all the fields and
     * values in a hash Group: hash Requires Redis 2.0.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> hgetall(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/hkeys">HKEYS</a>. Summary: Get all the fields in a hash
     * Group: hash Requires Redis 2.0.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> hkeys(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/hlen">HLEN</a>. Summary: Get the number of fields in a
     * hash Group: hash Requires Redis 2.0.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> hlen(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/hmget">HMGET</a>. Summary: Get the values of all the given
     * hash fields Group: hash Requires Redis 2.0.0
     *
     * @param key
     *        the key
     * @param fields
     *        the fields
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> hmget(K key, F... fields);

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
    Uni<Void> hmset(K key, Map<F, V> map);

    /**
     * Execute the command <a href="https://redis.io/commands/hrandfield">HRANDFIELD</a>. Summary: Get one or multiple
     * random fields from a hash Group: hash Requires Redis 6.2.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> hrandfield(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/hrandfield">HRANDFIELD</a>. Summary: Get one or multiple
     * random fields from a hash Group: hash Requires Redis 6.2.0
     *
     * @param key
     *        the key
     * @param count
     *        the number of random key to retrieve. If {@code count} is positive, the selected keys are distinct. If
     *        {@code count} is negative, the produced list can contain duplicated keys.
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> hrandfield(K key, long count);

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
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> hrandfieldWithValues(K key, long count);

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
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> hset(K key, F field, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/hset">HSET</a>. Summary: Set the string value of a hash
     * field Group: hash Requires Redis 2.0.0
     *
     * @param key
     *        the key
     * @param map
     *        the set of key -> value to add to the hash
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> hset(K key, Map<F, V> map);

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
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> hsetnx(K key, F field, V value);

    /**
     * Execute the command <a href="https://redis.io/commands/hstrlen">HSTRLEN</a>. Summary: Get the length of the value
     * of a hash field Group: hash Requires Redis 3.2.0
     *
     * @param key
     *        the key
     * @param field
     *        the value
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> hstrlen(K key, F field);

    /**
     * Execute the command <a href="https://redis.io/commands/hvals">HVALS</a>. Summary: Get all the values in a hash
     * Group: hash Requires Redis 2.0.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> hvals(K key);
}
