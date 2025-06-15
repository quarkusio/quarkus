package io.quarkus.redis.datasource.keys;

import java.time.Duration;
import java.time.Instant;

import io.quarkus.redis.datasource.ReactiveTransactionalRedisCommands;
import io.smallrye.mutiny.Uni;

public interface ReactiveTransactionalKeyCommands<K> extends ReactiveTransactionalRedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/copy">COPY</a>. Summary: Copy a key Group: generic
     * Requires Redis 6.2.0
     *
     * @param source
     *        the key
     * @param destination
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> copy(K source, K destination);

    /**
     * Execute the command <a href="https://redis.io/commands/copy">COPY</a>. Summary: Copy a key Group: generic
     * Requires Redis 6.2.0
     *
     * @param source
     *        the key
     * @param destination
     *        the key
     * @param copyArgs
     *        the additional arguments
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> copy(K source, K destination, CopyArgs copyArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/del">DEL</a>. Summary: Delete one or multiple keys Group:
     * generic Requires Redis 1.0.0
     *
     * @param keys
     *        the keys.
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> del(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/dump">DUMP</a>. Summary: Return a serialized version of
     * the value stored at the specified key. Group: generic Requires Redis 2.6.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> dump(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/exists">EXISTS</a>. Summary: Determine if a key exists
     * Group: generic Requires Redis 1.0.0
     *
     * @param key
     *        the key to check
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> exists(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/exists">EXISTS</a>. Summary: Determine if a key exists
     * Group: generic Requires Redis 1.0.0
     *
     * @param keys
     *        the keys to check
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> exists(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/expire">EXPIRE</a>. Summary: Set a key's time to live in
     * seconds Group: generic Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param seconds
     *        the new TTL
     * @param expireArgs
     *        the {@code EXPIRE} command extra-arguments
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> expire(K key, long seconds, ExpireArgs expireArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/expire">EXPIRE</a>. Summary: Set a key's time to live in
     * seconds Group: generic Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param duration
     *        the new TTL
     * @param expireArgs
     *        the {@code EXPIRE} command extra-arguments
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> expire(K key, Duration duration, ExpireArgs expireArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/expire">EXPIRE</a>. Summary: Set a key's time to live in
     * seconds Group: generic Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param seconds
     *        the new TTL
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> expire(K key, long seconds);

    /**
     * Execute the command <a href="https://redis.io/commands/expire">EXPIRE</a>. Summary: Set a key's time to live in
     * seconds Group: generic Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param duration
     *        the new TTL
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> expire(K key, Duration duration);

    /**
     * Execute the command <a href="https://redis.io/commands/expireat">EXPIREAT</a>. Summary: Set the expiration for a
     * key as a UNIX timestamp Group: generic Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param timestamp
     *        the timestamp
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> expireat(K key, long timestamp);

    /**
     * Execute the command <a href="https://redis.io/commands/expireat">EXPIREAT</a>. Summary: Set the expiration for a
     * key as a UNIX timestamp Group: generic Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param timestamp
     *        the timestamp
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> expireat(K key, Instant timestamp);

    /**
     * Execute the command <a href="https://redis.io/commands/expireat">EXPIREAT</a>. Summary: Set the expiration for a
     * key as a UNIX timestamp Group: generic Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param timestamp
     *        the timestamp
     * @param expireArgs
     *        the {@code EXPIREAT} command extra-arguments
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> expireat(K key, long timestamp, ExpireArgs expireArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/expireat">EXPIREAT</a>. Summary: Set the expiration for a
     * key as a UNIX timestamp Group: generic Requires Redis 1.2.0
     *
     * @param key
     *        the key
     * @param timestamp
     *        the timestamp
     * @param expireArgs
     *        the {@code EXPIREAT} command extra-arguments
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> expireat(K key, Instant timestamp, ExpireArgs expireArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/expiretime">EXPIRETIME</a>. Summary: Get the expiration
     * Unix timestamp for a key Group: generic Requires Redis 7.0.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     *
     * @throws RedisKeyNotFoundException
     *         if the key does not exist
     */
    Uni<Void> expiretime(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/keys">KEYS</a>. Summary: Find all keys matching the given
     * pattern Group: generic Requires Redis 1.0.0
     *
     * @param pattern
     *        the glob-style pattern
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> keys(String pattern);

    /**
     * Execute the command <a href="https://redis.io/commands/move">MOVE</a>. Summary: Move a key to another database
     * Group: generic Requires Redis 1.0.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> move(K key, long db);

    /**
     * Execute the command <a href="https://redis.io/commands/persist">PERSIST</a>. Summary: Remove the expiration from
     * a key Group: generic Requires Redis 2.2.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> persist(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/pexpire">PEXPIRE</a>. Summary: Set a key's time to live in
     * milliseconds Group: generic Requires Redis 2.6.0
     *
     * @param key
     *        the key
     * @param duration
     *        the new TTL
     * @param expireArgs
     *        the {@code PEXPIRE} command extra-arguments
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> pexpire(K key, Duration duration, ExpireArgs expireArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/pexpire">PEXPIRE</a>. Summary: Set a key's time to live in
     * milliseconds Group: generic Requires Redis 2.6.0
     *
     * @param key
     *        the key
     * @param ms
     *        the new TTL
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> pexpire(K key, long ms);

    /**
     * Execute the command <a href="https://redis.io/commands/pexpire">PEXPIRE</a>. Summary: Set a key's time to live in
     * milliseconds Group: generic Requires Redis 2.6.0
     *
     * @param key
     *        the key
     * @param duration
     *        the new TTL
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> pexpire(K key, Duration duration);

    /**
     * Execute the command <a href="https://redis.io/commands/pexpire">PEXPIRE</a>. Summary: Set a key's time to live in
     * milliseconds Group: generic Requires Redis 2.6.0
     *
     * @param key
     *        the key
     * @param milliseconds
     *        the new TTL
     * @param expireArgs
     *        the {@code PEXPIRE} command extra-arguments
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> pexpire(K key, long milliseconds, ExpireArgs expireArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/pexpireat">PEXPIREAT</a>. Summary: Set the expiration for
     * a key as a UNIX timestamp Group: generic Requires Redis 2.6.0
     *
     * @param key
     *        the key
     * @param timestamp
     *        the timestamp
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> pexpireat(K key, long timestamp);

    /**
     * Execute the command <a href="https://redis.io/commands/pexpireat">PEXPIREAT</a>. Summary: Set the expiration for
     * a key as a UNIX timestamp Group: generic Requires Redis 2.6.0
     *
     * @param key
     *        the key
     * @param timestamp
     *        the timestamp
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> pexpireat(K key, Instant timestamp);

    /**
     * Execute the command <a href="https://redis.io/commands/pexpireat">PEXPIREAT</a>. Summary: Set the expiration for
     * a key as a UNIX timestamp Group: generic Requires Redis 2.6.0
     *
     * @param key
     *        the key
     * @param timestamp
     *        the timestamp
     * @param expireArgs
     *        the {@code EXPIREAT} command extra-arguments
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> pexpireat(K key, long timestamp, ExpireArgs expireArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/pexpireat">PEXPIREAT</a>. Summary: Set the expiration for
     * a key as a UNIX timestamp Group: generic Requires Redis 2.6.0
     *
     * @param key
     *        the key
     * @param timestamp
     *        the timestamp
     * @param expireArgs
     *        the {@code EXPIREAT} command extra-arguments
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> pexpireat(K key, Instant timestamp, ExpireArgs expireArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/pexpiretime">PEXPIRETIME</a>. Summary: Get the expiration
     * Unix timestamp for a key Group: generic Requires Redis 2.6.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     *
     * @throws RedisKeyNotFoundException
     *         if the key does not exist
     */
    Uni<Void> pexpiretime(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/pttl">PTTL</a>. Summary: Get the time to live for a key in
     * milliseconds Group: generic Requires Redis 2.6.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> pttl(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/randomkey">RANDOMKEY</a>. Summary: Return a random key
     * from the keyspace Group: generic Requires Redis 1.0.0
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> randomkey();

    /**
     * Execute the command <a href="https://redis.io/commands/rename">RENAME</a>. Summary: Rename a key Group: generic
     * Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param newkey
     *        the new key
     */
    Uni<Void> rename(K key, K newkey);

    /**
     * Execute the command <a href="https://redis.io/commands/renamenx">RENAMENX</a>. Summary: Rename a key, only if the
     * new key does not exist Group: generic Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param newkey
     *        the new key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> renamenx(K key, K newkey);

    /**
     * Execute the command <a href="https://redis.io/commands/touch">TOUCH</a>. Summary: Alters the last access time of
     * a key(s). Returns the number of existing keys specified. Group: generic Requires Redis 3.2.1
     *
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> touch(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/ttl">TTL</a>. Summary: Get the time to live for a key in
     * seconds Group: generic Requires Redis 1.0.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     *
     * @throws RedisKeyNotFoundException
     *         if the key does not exist
     */
    Uni<Void> ttl(K key) throws RedisKeyNotFoundException;

    /**
     * Execute the command <a href="https://redis.io/commands/type">TYPE</a>. Summary: Determine the type stored at key
     * Group: generic Requires Redis 1.0.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> type(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/unlink">UNLINK</a>. Summary: Delete a key asynchronously
     * in another thread. Otherwise, it is just as {@code DEL}, but non-blocking. Group: generic Requires Redis 4.0.0
     *
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> unlink(K... keys);
}
