package io.quarkus.redis.datasource.bitmap;

import java.util.List;

import io.quarkus.redis.datasource.ReactiveRedisCommands;
import io.smallrye.mutiny.Uni;

/**
 * Allows executing commands from the {@code bitmap} group. See <a href="https://redis.io/commands/?group=bitmap">the
 * bitmap command list</a> for further information about these commands.
 *
 * @param <K>
 *        the type of the key
 */
public interface ReactiveBitMapCommands<K> extends ReactiveRedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/bitcount">BITCOUNT</a>. Summary: Count set bits in a
     * string Group: bitmap Requires Redis 2.6.0
     *
     * @param key
     *        the key
     *
     * @return the number of bits set to 1 in the stored string.
     **/
    Uni<Long> bitcount(K key);

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
     * @return the number of bits set to 1 in the stored string.
     **/
    Uni<Long> bitcount(K key, long start, long end);

    /**
     * Returns the bit value at offset in the string value stored at key.
     *
     * @param key
     *        the key.
     * @param offset
     *        the offset
     *
     * @return the bit value stored at <em>offset</em> (0 or 1).
     */
    Uni<Integer> getbit(K key, long offset);

    /**
     * Execute the command <a href="https://redis.io/commands/bitfield">BITFIELD</a>. Summary: Perform arbitrary
     * bitfield integer operations on strings Group: bitmap Requires Redis 3.2.0
     *
     * @param key
     *        the key
     *
     * @return the results from the bitfield commands as described on https://redis.io/commands/bitfield/.
     **/
    Uni<List<Long>> bitfield(K key, BitFieldArgs bitFieldArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/bitpos">BITPOS</a>. Summary: Find first bit set or clear
     * in a string Group: bitmap Requires Redis 2.8.7
     *
     * @param key
     *        the key
     * @param bit
     *        {@code 1} to look for {@code 1}, {@code 0} to look for {@code 0}
     *
     * @return the position of the first bit set to 1 or 0 according to the request. If we look for set bits (the bit
     *         argument is 1) and the string is empty or composed of just zero ints, -1 is returned. If we look for
     *         clear bits (the bit argument is 0) and the string only contains bit set to 1, the function returns the
     *         first bit not part of the string on the right. So if the string is three ints set to the value 0xff the
     *         command BITPOS key 0 will return 24, since up to bit 23 all the bits are 1. Basically, the function
     *         considers the right of the string as padded with zeros if you look for clear bits and specify no range or
     *         the start argument only. However, this behavior changes if you are looking for clear bits and specify a
     *         range with both start and end. If no clear bit is found in the specified range, the function returns -1
     *         as the user specified a clear range and there are no 0 bits in that range.
     **/
    Uni<Long> bitpos(K key, int bit);

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
     * @return the position of the first bit set to 1 or 0 according to the request. If we look for set bits (the bit
     *         argument is 1) and the string is empty or composed of just zero ints, -1 is returned. If we look for
     *         clear bits (the bit argument is 0) and the string only contains bit set to 1, the function returns the
     *         first bit not part of the string on the right. So if the string is three ints set to the value 0xff the
     *         command BITPOS key 0 will return 24, since up to bit 23 all the bits are 1. Basically, the function
     *         considers the right of the string as padded with zeros if you look for clear bits and specify no range or
     *         the start argument only. However, this behavior changes if you are looking for clear bits and specify a
     *         range with both start and end. If no clear bit is found in the specified range, the function returns -1
     *         as the user specified a clear range and there are no 0 bits in that range.
     **/
    Uni<Long> bitpos(K key, int bit, long start);

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
     * @param end
     *        the end position
     *
     * @return the position of the first bit set to 1 or 0 according to the request. If we look for set bits (the bit
     *         argument is 1) and the string is empty or composed of just zero ints, -1 is returned. If we look for
     *         clear bits (the bit argument is 0) and the string only contains bit set to 1, the function returns the
     *         first bit not part of the string on the right. So if the string is three ints set to the value 0xff the
     *         command BITPOS key 0 will return 24, since up to bit 23 all the bits are 1. Basically, the function
     *         considers the right of the string as padded with zeros if you look for clear bits and specify no range or
     *         the start argument only. However, this behavior changes if you are looking for clear bits and specify a
     *         range with both start and end. If no clear bit is found in the specified range, the function returns -1
     *         as the user specified a clear range and there are no 0 bits in that range.
     **/
    Uni<Long> bitpos(K key, int bit, long start, long end);

    /**
     * Execute the command <a href="https://redis.io/commands/bitop">BITOP</a>. Summary: Perform a bitwise AND operation
     * between strings Group: bitmap Requires Redis 2.6.0
     *
     * @param destination
     *        the destination key
     * @param keys
     *        the keys
     *
     * @return The size of the string stored in the destination key, that is equal to the size of the longest input
     *         string.
     **/
    Uni<Long> bitopAnd(K destination, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/bitop">BITOP</a>. Summary: Perform a bitwise NOT operation
     * between strings Group: bitmap Requires Redis 2.6.0
     *
     * @param destination
     *        the destination key
     * @param source
     *        the source key
     *
     * @return The size of the string stored in the destination key, that is equal to the size of the longest input
     *         string.
     **/
    Uni<Long> bitopNot(K destination, K source);

    /**
     * Execute the command <a href="https://redis.io/commands/bitop">BITOP</a>. Summary: Perform a bitwise OR operation
     * between strings Group: bitmap Requires Redis 2.6.0
     *
     * @param destination
     *        the destination key
     * @param keys
     *        the keys
     *
     * @return The size of the string stored in the destination key, that is equal to the size of the longest input
     *         string.
     **/
    Uni<Long> bitopOr(K destination, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/bitop">BITOP</a>. Summary: Perform a bitwise XOR operation
     * between strings Group: bitmap Requires Redis 2.6.0
     *
     * @param destination
     *        the destination key
     * @param keys
     *        the keys
     *
     * @return The size of the string stored in the destination key, that is equal to the size of the longest input
     *         string.
     **/
    Uni<Long> bitopXor(K destination, K... keys);

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
     * @return the original bit value stored at <em>offset</em>, 0 or 1.
     */
    Uni<Integer> setbit(K key, long offset, int value);
}
