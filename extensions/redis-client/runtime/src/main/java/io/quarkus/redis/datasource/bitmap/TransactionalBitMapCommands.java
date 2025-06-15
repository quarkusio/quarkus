package io.quarkus.redis.datasource.bitmap;

import io.quarkus.redis.datasource.TransactionalRedisCommands;

/**
 * Allows executing commands from the {@code bitmap} group in a Redis transaction ({@code Multi}). See
 * <a href="https://redis.io/commands/?group=bitmap">the bitmap command list</a> for further information about these
 * commands.
 *
 * @param <K>
 *        the type of the key
 */
public interface TransactionalBitMapCommands<K> extends TransactionalRedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/bitcount">BITCOUNT</a>. Summary: Count set bits in a
     * string Group: bitmap Requires Redis 2.6.0
     *
     * @param key
     *        the key
     **/
    void bitcount(K key);

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
     **/
    void bitcount(K key, long start, long end);

    /**
     * Returns the bit value at offset in the string value stored at key.
     *
     * @param key
     *        the key.
     * @param offset
     *        the offset
     */
    void getbit(K key, long offset);

    /**
     * Execute the command <a href="https://redis.io/commands/bitfield">BITFIELD</a>. Summary: Perform arbitrary
     * bitfield integer operations on strings Group: bitmap Requires Redis 3.2.0
     *
     * @param key
     *        the key
     **/
    void bitfield(K key, BitFieldArgs bitFieldArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/bitpos">BITPOS</a>. Summary: Find first bit set or clear
     * in a string Group: bitmap Requires Redis 2.8.7
     *
     * @param key
     *        the key
     * @param valueToLookFor
     *        {@code 1} to look for {@code 1}, {@code 0} to look for {@code 0}
     **/
    void bitpos(K key, int valueToLookFor);

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
     **/
    void bitpos(K key, int bit, long start);

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
     **/
    void bitpos(K key, int bit, long start, long end);

    /**
     * Execute the command <a href="https://redis.io/commands/bitop">BITOP</a>. Summary: Perform a bitwise AND operation
     * between strings Group: bitmap Requires Redis 2.6.0
     *
     * @param destination
     *        the destination key
     * @param keys
     *        the keys
     **/
    void bitopAnd(K destination, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/bitop">BITOP</a>. Summary: Perform a bitwise NOT operation
     * between strings Group: bitmap Requires Redis 2.6.0
     *
     * @param destination
     *        the destination key
     * @param source
     *        the source key
     **/
    void bitopNot(K destination, K source);

    /**
     * Execute the command <a href="https://redis.io/commands/bitop">BITOP</a>. Summary: Perform a bitwise OR operation
     * between strings Group: bitmap Requires Redis 2.6.0
     *
     * @param destination
     *        the destination key
     * @param keys
     *        the keys
     **/
    void bitopOr(K destination, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/bitop">BITOP</a>. Summary: Perform a bitwise XOR operation
     * between strings Group: bitmap Requires Redis 2.6.0
     *
     * @param destination
     *        the destination key
     * @param keys
     *        the keys
     **/
    void bitopXor(K destination, K... keys);

    /**
     * Sets or clears the bit at offset in the string value stored at key.
     *
     * @param key
     *        the key.
     * @param offset
     *        the offset
     * @param value
     *        the value (O or 1)
     */
    void setbit(K key, long offset, int value);
}
