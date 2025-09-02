package io.quarkus.redis.datasource.keys;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommands;

/**
 * Allows executing commands manipulating keys.
 * See <a href="https://redis.io/commands/?group=generic">the generic command list</a> for further information about these
 * commands.
 * <p>
 *
 * @param <K> the type of the keys, often {@link String}
 */
public interface KeyCommands<K> extends RedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/copy">COPY</a>.
     * Summary: Copy a key
     * Group: generic
     * Requires Redis 6.2.0
     *
     * @param source the key
     * @param destination the key
     * @return {@code true} source was copied. {@code false} source was not copied.
     **/
    boolean copy(K source, K destination);

    /**
     * Execute the command <a href="https://redis.io/commands/copy">COPY</a>.
     * Summary: Copy a key
     * Group: generic
     * Requires Redis 6.2.0
     *
     * @param source the key
     * @param destination the key
     * @param copyArgs the additional arguments
     * @return {@code true} source was copied. {@code false} source was not copied.
     **/
    boolean copy(K source, K destination, CopyArgs copyArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/del">DEL</a>.
     * Summary: Delete one or multiple keys
     * Group: generic
     * Requires Redis 1.0.0
     *
     * @param keys the keys.
     * @return The number of keys that were removed.
     **/
    int del(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/dump">DUMP</a>.
     * Summary: Return a serialized version of the value stored at the specified key.
     * Group: generic
     * Requires Redis 2.6.0
     *
     * @param key the key
     * @return the serialized value.
     **/
    String dump(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/exists">EXISTS</a>.
     * Summary: Determine if a key exists
     * Group: generic
     * Requires Redis 1.0.0
     *
     * @param key the key to check
     * @return {@code true} if the key exists, {@code false} otherwise
     **/
    boolean exists(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/exists">EXISTS</a>.
     * Summary: Determine if a key exists
     * Group: generic
     * Requires Redis 1.0.0
     *
     * @param keys the keys to check
     * @return the number of keys that exist from those specified as arguments.
     **/
    int exists(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/expire">EXPIRE</a>.
     * Summary: Set a key's time to live in seconds
     * Group: generic
     * Requires Redis 1.0.0
     *
     * @param key the key
     * @param seconds the new TTL
     * @param expireArgs the {@code EXPIRE} command extra-arguments
     * @return {@code true} the timeout was set. {@code false} the timeout was not set. e.g. key doesn't exist,
     *         or operation skipped due to the provided arguments.
     **/
    boolean expire(K key, long seconds, ExpireArgs expireArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/expire">EXPIRE</a>.
     * Summary: Set a key's time to live in seconds
     * Group: generic
     * Requires Redis 1.0.0
     *
     * @param key the key
     * @param duration the new TTL
     * @param expireArgs the {@code EXPIRE} command extra-arguments
     * @return {@code true} the timeout was set. {@code false} the timeout was not set. e.g. key doesn't exist,
     *         or operation skipped due to the provided arguments.
     **/
    boolean expire(K key, Duration duration, ExpireArgs expireArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/expire">EXPIRE</a>.
     * Summary: Set a key's time to live in seconds
     * Group: generic
     * Requires Redis 1.0.0
     *
     * @param key the key
     * @param seconds the new TTL
     * @return {@code true} the timeout was set. {@code false} the timeout was not set. e.g. key doesn't exist.
     **/
    boolean expire(K key, long seconds);

    /**
     * Execute the command <a href="https://redis.io/commands/expire">EXPIRE</a>.
     * Summary: Set a key's time to live in seconds
     * Group: generic
     * Requires Redis 1.0.0
     *
     * @param key the key
     * @param duration the new TTL
     * @return {@code true} the timeout was set. {@code false} the timeout was not set. e.g. key doesn't exist.
     **/
    boolean expire(K key, Duration duration);

    /**
     * Execute the command <a href="https://redis.io/commands/expireat">EXPIREAT</a>.
     * Summary: Set the expiration for a key as a UNIX timestamp
     * Group: generic
     * Requires Redis 1.2.0
     *
     * @param key the key
     * @param timestamp the timestamp
     * @return {@code true} the timeout was set. {@code false} the timeout was not set. e.g. key doesn't exist.
     **/
    boolean expireat(K key, long timestamp);

    /**
     * Execute the command <a href="https://redis.io/commands/expireat">EXPIREAT</a>.
     * Summary: Set the expiration for a key as a UNIX timestamp
     * Group: generic
     * Requires Redis 1.2.0
     *
     * @param key the key
     * @param timestamp the timestamp
     * @return {@code true} the timeout was set. {@code false} the timeout was not set. e.g. key doesn't exist.
     **/
    boolean expireat(K key, Instant timestamp);

    /**
     * Execute the command <a href="https://redis.io/commands/expireat">EXPIREAT</a>.
     * Summary: Set the expiration for a key as a UNIX timestamp
     * Group: generic
     * Requires Redis 1.2.0
     *
     * @param key the key
     * @param timestamp the timestamp
     * @param expireArgs the {@code EXPIREAT} command extra-arguments
     * @return {@code true} the timeout was set. {@code false} the timeout was not set. e.g. key doesn't exist, or
     *         operation skipped due to the provided arguments.
     **/
    boolean expireat(K key, long timestamp, ExpireArgs expireArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/expireat">EXPIREAT</a>.
     * Summary: Set the expiration for a key as a UNIX timestamp
     * Group: generic
     * Requires Redis 1.2.0
     *
     * @param key the key
     * @param timestamp the timestamp
     * @param expireArgs the {@code EXPIREAT} command extra-arguments
     * @return {@code true} the timeout was set. {@code false} the timeout was not set. e.g. key doesn't exist, or
     *         operation skipped due to the provided arguments.
     **/
    boolean expireat(K key, Instant timestamp, ExpireArgs expireArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/expiretime">EXPIRETIME</a>.
     * Summary: Get the expiration Unix timestamp for a key
     * Group: generic
     * Requires Redis 7.0.0
     *
     * @param key the key
     * @return the expiration Unix timestamp in seconds, {@code -1} if the key exists but has no associated expiration
     *         time.
     * @throws RedisKeyNotFoundException if the key does not exist
     **/
    long expiretime(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/keys">KEYS</a>.
     * Summary: Find all keys matching the given pattern
     * Group: generic
     * Requires Redis 1.0.0
     *
     * @param pattern the glob-style pattern
     * @return the list of keys matching pattern.
     **/
    List<K> keys(String pattern);

    /**
     * Execute the command <a href="https://redis.io/commands/move">MOVE</a>.
     * Summary: Move a key to another database
     * Group: generic
     * Requires Redis 1.0.0
     *
     * @param key the key
     * @return {@code true} key was moved. {@code false} key was not moved.
     **/
    boolean move(K key, long db);

    /**
     * Execute the command <a href="https://redis.io/commands/persist">PERSIST</a>.
     * Summary: Remove the expiration from a key
     * Group: generic
     * Requires Redis 2.2.0
     *
     * @param key the key
     * @return {@code true} the timeout was removed. {@code false} key does not exist or does not have an associated timeout.
     **/
    boolean persist(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/pexpire">PEXPIRE</a>.
     * Summary: Set a key's time to live in milliseconds
     * Group: generic
     * Requires Redis 2.6.0
     *
     * @param key the key
     * @param duration the new TTL
     * @param expireArgs the {@code PEXPIRE} command extra-arguments
     * @return {@code true} the timeout was set. {@code false} the timeout was not set. e.g. key doesn't exist,
     *         or operation skipped due to the provided arguments.
     **/
    boolean pexpire(K key, Duration duration, ExpireArgs expireArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/pexpire">PEXPIRE</a>.
     * Summary: Set a key's time to live in milliseconds
     * Group: generic
     * Requires Redis 2.6.0
     *
     * @param key the key
     * @param ms the new TTL
     * @return {@code true} the timeout was set. {@code false} the timeout was not set. e.g. key doesn't exist.
     **/
    boolean pexpire(K key, long ms);

    /**
     * Execute the command <a href="https://redis.io/commands/pexpire">PEXPIRE</a>.
     * Summary: Set a key's time to live in milliseconds
     * Group: generic
     * Requires Redis 2.6.0
     *
     * @param key the key
     * @param duration the new TTL
     * @return {@code true} the timeout was set. {@code false} the timeout was not set. e.g. key doesn't exist.
     **/
    boolean pexpire(K key, Duration duration);

    /**
     * Execute the command <a href="https://redis.io/commands/pexpire">PEXPIRE</a>.
     * Summary: Set a key's time to live in milliseconds
     * Group: generic
     * Requires Redis 2.6.0
     *
     * @param key the key
     * @param milliseconds the new TTL
     * @param expireArgs the {@code PEXPIRE} command extra-arguments
     * @return {@code true} the timeout was set. {@code false} the timeout was not set. e.g. key doesn't exist.
     **/
    boolean pexpire(K key, long milliseconds, ExpireArgs expireArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/pexpireat">PEXPIREAT</a>.
     * Summary: Set the expiration for a key as a UNIX timestamp
     * Group: generic
     * Requires Redis 2.6.0
     *
     * @param key the key
     * @param timestamp the timestamp
     * @return {@code true} the timeout was set. {@code false} the timeout was not set. e.g. key doesn't exist.
     **/
    boolean pexpireat(K key, long timestamp);

    /**
     * Execute the command <a href="https://redis.io/commands/pexpireat">PEXPIREAT</a>.
     * Summary: Set the expiration for a key as a UNIX timestamp
     * Group: generic
     * Requires Redis 2.6.0
     *
     * @param key the key
     * @param timestamp the timestamp
     * @return {@code true} the timeout was set. {@code false} the timeout was not set. e.g. key doesn't exist.
     **/
    boolean pexpireat(K key, Instant timestamp);

    /**
     * Execute the command <a href="https://redis.io/commands/pexpireat">PEXPIREAT</a>.
     * Summary: Set the expiration for a key as a UNIX timestamp
     * Group: generic
     * Requires Redis 2.6.0
     *
     * @param key the key
     * @param timestamp the timestamp
     * @param expireArgs the {@code EXPIREAT} command extra-arguments
     * @return {@code true} the timeout was set. {@code false} the timeout was not set. e.g. key doesn't exist, or
     *         operation skipped due to the provided arguments.
     **/
    boolean pexpireat(K key, long timestamp, ExpireArgs expireArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/pexpireat">PEXPIREAT</a>.
     * Summary: Set the expiration for a key as a UNIX timestamp
     * Group: generic
     * Requires Redis 2.6.0
     *
     * @param key the key
     * @param timestamp the timestamp
     * @param expireArgs the {@code EXPIREAT} command extra-arguments
     * @return {@code true} the timeout was set. {@code false} the timeout was not set. e.g. key doesn't exist, or
     *         operation skipped due to the provided arguments.
     **/
    boolean pexpireat(K key, Instant timestamp, ExpireArgs expireArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/pexpiretime">PEXPIRETIME</a>.
     * Summary: Get the expiration Unix timestamp for a key
     * Group: generic
     * Requires Redis 2.6.0
     *
     * @param key the key
     * @return the expiration Unix timestamp in milliseconds; {@code -1} if the key exists but has no associated
     *         expiration time.
     * @throws RedisKeyNotFoundException if the key does not exist
     **/
    long pexpiretime(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/pttl">PTTL</a>.
     * Summary: Get the time to live for a key in milliseconds
     * Group: generic
     * Requires Redis 2.6.0
     *
     * @param key the key
     * @return TTL in milliseconds. The command returns {@code -1} if the key exists but has no associated expire.
     * @throws RedisKeyNotFoundException if the key does not exist
     **/
    long pttl(K key) throws RedisKeyNotFoundException;

    /**
     * Execute the command <a href="https://redis.io/commands/randomkey">RANDOMKEY</a>.
     * Summary: Return a random key from the keyspace
     * Group: generic
     * Requires Redis 1.0.0
     *
     * @return the random key, or {@code null} when the database is empty.
     **/
    K randomkey();

    /**
     * Execute the command <a href="https://redis.io/commands/rename">RENAME</a>.
     * Summary: Rename a key
     * Group: generic
     * Requires Redis 1.0.0
     *
     * @param key the key
     * @param newkey the new key
     **/
    void rename(K key, K newkey);

    /**
     * Execute the command <a href="https://redis.io/commands/renamenx">RENAMENX</a>.
     * Summary: Rename a key, only if the new key does not exist
     * Group: generic
     * Requires Redis 1.0.0
     *
     * @param key the key
     * @param newkey the new key
     * @return {@code true} if {@code key} was renamed to {@code newkey}. {@code false} if {@code newkey} already exists.
     **/
    boolean renamenx(K key, K newkey);

    /**
     * Execute the command <a href="https://redis.io/commands/scan">SCAN</a>.
     * Summary: Incrementally iterate the keys space
     * Group: generic
     * Requires Redis 2.8.0
     *
     * @return the cursor.
     **/
    KeyScanCursor<K> scan();

    /**
     * Execute the command <a href="https://redis.io/commands/scan">SCAN</a>.
     * Summary: Incrementally iterate the keys space
     * Group: generic
     * Requires Redis 2.8.0
     *
     * @param args the extra arguments
     * @return the cursor.
     **/
    KeyScanCursor<K> scan(KeyScanArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/touch">TOUCH</a>.
     * Summary: Alters the last access time of a key(s). Returns the number of existing keys specified.
     * Group: generic
     * Requires Redis 3.2.1
     *
     * @param keys the keys
     * @return The number of keys that were touched.
     **/
    int touch(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/ttl">TTL</a>.
     * Summary: Get the time to live for a key in seconds
     * Group: generic
     * Requires Redis 1.0.0
     *
     * @param key the key
     * @return TTL in seconds, {@code -1} if the key exists but has no associated expire.
     * @throws RedisKeyNotFoundException if the key does not exist
     **/
    long ttl(K key) throws RedisKeyNotFoundException;

    /**
     * Execute the command <a href="https://redis.io/commands/type">TYPE</a>.
     * Summary: Determine the type stored at key
     * Group: generic
     * Requires Redis 1.0.0
     *
     * @param key the key
     * @return type of key, or {@code NONE} when key does not exist.
     **/
    RedisValueType type(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/unlink">UNLINK</a>.
     * Summary: Delete a key asynchronously in another thread. Otherwise, it is just as {@code DEL}, but non-blocking.
     * Group: generic
     * Requires Redis 4.0.0
     *
     * @param keys the keys
     * @return The number of keys that were unlinked.
     **/
    int unlink(K... keys);

}
