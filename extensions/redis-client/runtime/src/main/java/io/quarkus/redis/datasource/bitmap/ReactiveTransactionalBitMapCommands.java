package io.quarkus.redis.datasource.bitmap;

import io.quarkus.redis.datasource.ReactiveTransactionalRedisCommands;
import io.smallrye.mutiny.Uni;

public interface ReactiveTransactionalBitMapCommands<K> extends ReactiveTransactionalRedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/bitcount">BITCOUNT</a>. Summary: Count set bits in a
     * string Group: bitmap Requires Redis 2.6.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> bitcount(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/bitcount">BITCOUNT</a>. Summary: Count set bits in a
     * string Group: bitmap Requires Redis 2.6.0
     *
     * @param key
     *        the key
     * @param start
     *        the start index
     * @param end
     *        the end index
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> bitcount(K key, long start, long end);

    /**
     * Returns the bit value at offset in the string value stored at key.
     *
     * @param key
     *        the key.
     * @param offset
     *        the offset
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> getbit(K key, long offset);

    /**
     * Execute the command <a href="https://redis.io/commands/bitfield">BITFIELD</a>. Summary: Perform arbitrary
     * bitfield integer operations on strings Group: bitmap Requires Redis 3.2.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> bitfield(K key, BitFieldArgs bitFieldArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/bitpos">BITPOS</a>. Summary: Find first bit set or clear
     * in a string Group: bitmap Requires Redis 2.8.7
     *
     * @param key
     *        the key
     * @param valueToLookFor
     *        {@code 1} to look for {@code 1}, {@code 0} to look for {@code 0}
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> bitpos(K key, int valueToLookFor);

    /**
     * Execute the command <a href="https://redis.io/commands/bitpos">BITPOS</a>. Summary: Find first bit set or clear
     * in a string Group: bitmap Requires Redis 2.8.7
     *
     * @param key
     *        the key
     * @param bit
     *        {@code 1} to look for {@code 1}, {@code 0} to look for {@code 0}
     * @param start
     *        the start position
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> bitpos(K key, int bit, long start);

    /**
     * Execute the command <a href="https://redis.io/commands/bitpos">BITPOS</a>. Summary: Find first bit set or clear
     * in a string Group: bitmap Requires Redis 2.8.7
     *
     * @param key
     *        the key
     * @param bit
     *        {@code true} to look for {@code 1}, {@code false} to look for {@code 0}
     * @param start
     *        the start position
     * @param end
     *        the end position
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> bitpos(K key, int bit, long start, long end);

    /**
     * Execute the command <a href="https://redis.io/commands/bitop">BITOP</a>. Summary: Perform a bitwise AND operation
     * between strings Group: bitmap Requires Redis 2.6.0
     *
     * @param destination
     *        the destination key
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> bitopAnd(K destination, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/bitop">BITOP</a>. Summary: Perform a bitwise NOT operation
     * between strings Group: bitmap Requires Redis 2.6.0
     *
     * @param destination
     *        the destination key
     * @param source
     *        the source key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> bitopNot(K destination, K source);

    /**
     * Execute the command <a href="https://redis.io/commands/bitop">BITOP</a>. Summary: Perform a bitwise OR operation
     * between strings Group: bitmap Requires Redis 2.6.0
     *
     * @param destination
     *        the destination key
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> bitopOr(K destination, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/bitop">BITOP</a>. Summary: Perform a bitwise XOR operation
     * between strings Group: bitmap Requires Redis 2.6.0
     *
     * @param destination
     *        the destination key
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> bitopXor(K destination, K... keys);

    /**
     * Sets or clears the bit at offset in the string value stored at key.
     *
     * @param key
     *        the key.
     * @param offset
     *        the offset
     * @param value
     *        the value (O or 1)
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> setbit(K key, long offset, int value);
}
