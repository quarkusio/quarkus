package io.quarkus.redis.datasource.bitmap;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

public class BitFieldArgs implements RedisCommandExtraArguments {

    public enum OverflowType {
        WRAP,
        SAT,
        FAIL;
    }

    /**
     * Represents a bit field type with details about signed/unsigned and the number of bits. Instances can be created
     * from a boolean/bits or from strings like i8 (signed) or u10 (unsigned).
     */
    public static class BitFieldType {

        public final boolean signed;
        public final int bits;

        public BitFieldType(String bit) {
            if (bit.startsWith("i")) {
                this.signed = true;
                this.bits = Integer.parseInt(bit.substring(1));
            } else if (bit.startsWith("u")) {
                this.signed = false;
                this.bits = Integer.parseInt(bit.substring(1));
            }
            throw new IllegalArgumentException("Invalid integer encoding for a bit field type: " + bit
                    + ". It must start with `i` (signed integers) or `u` (unsigned integers)");
        }

        public BitFieldType(boolean signed, int bits) {
            if (bits <= 0) {
                throw new IllegalArgumentException("`bits` must be strictly positive");
            }

            if (signed && bits >= 65) {
                throw new IllegalArgumentException("Signed integers support only up to 64 bits");
            }
            if (!signed && bits >= 64) {
                throw new IllegalArgumentException("Unsigned integers support only up to 63 bits");
            }

            this.signed = signed;
            this.bits = bits;
        }

        @Override
        public String toString() {
            return (signed ? "i" : "u") + bits;
        }

    }

    /**
     * Represents a bit field offset. See also
     * <a href="https://redis.io/commands/bitfield#bits-and-positional-offsets">Bits and positional offsets</a>
     */
    public static class Offset {

        public final boolean multiplyByTypeWidth;
        public final int offset;

        public Offset(String s) {
            if (s.startsWith("#")) {
                multiplyByTypeWidth = true;
                offset = Integer.parseInt(s.substring(1));
            } else {
                multiplyByTypeWidth = false;
                offset = Integer.parseInt(s);
            }
        }

        public Offset(boolean multiplyByTypeWidth, int offset) {
            this.multiplyByTypeWidth = multiplyByTypeWidth;
            this.offset = offset;
        }

        @Override
        public String toString() {
            return (multiplyByTypeWidth ? "#" : "") + offset;
        }

    }

    private final List<Object> commands = new ArrayList<>();
    private BitFieldType previousBitFieldType;

    /**
     * Adds a new {@code GET} subcommand using offset {@code 0} and the field type of the previous command.
     *
     * @return the current {@code BitFieldArgs}
     *
     * @throws IllegalStateException
     *         if no previous field type was found
     */
    public BitFieldArgs get() {
        return get(getPreviousFieldType(), 0);
    }

    /**
     * Adds a new {@code GET} subcommand using offset {@code 0}.
     *
     * @param bitFieldType
     *        the bit field type, must not be {@code null}.
     *
     * @return the current {@code BitFieldArgs}
     */
    public BitFieldArgs get(BitFieldType bitFieldType) {
        return get(bitFieldType, 0);
    }

    /**
     * Adds a new {@code GET} subcommand using the field type of the previous command.
     *
     * @param offset
     *        bitfield offset
     *
     * @return a new {@code GET} subcommand for the given {@code bitFieldType} and {@code offset}.
     *
     * @throws IllegalStateException
     *         if no previous field type was found
     */
    public BitFieldArgs get(int offset) {
        return get(getPreviousFieldType(), offset);
    }

    /**
     * Adds a new {@code GET} subcommand.
     *
     * @param bft
     *        the bit field type, must not be {@code null}.
     * @param offset
     *        bitfield offset
     *
     * @return the current {@code BitFieldArgs}
     */
    public BitFieldArgs get(BitFieldType bft, int offset) {
        return get(bft, new Offset(false, offset));
    }

    /**
     * Adds a new {@code GET} subcommand.
     *
     * @param bft
     *        the bit field type, must not be {@code null}.
     * @param offset
     *        bitfield offset
     *
     * @return the current {@code BitFieldArgs}
     */
    public BitFieldArgs get(BitFieldType bft, Offset offset) {
        if (offset == null) {
            throw new IllegalArgumentException("`offset` must not be `null`");
        }
        if (offset.offset < 0) {
            throw new IllegalArgumentException("`offset` must be greater or equal to 0");
        }

        this.previousBitFieldType = bft;
        this.commands.addAll(List.of("GET", bft.toString(), offset.toString()));
        return this;
    }

    /**
     * Adds a new {@code SET} subcommand.
     *
     * @param bft
     *        the bit field type, must not be {@code null}.
     * @param offset
     *        bitfield offset
     * @param value
     *        the value
     *
     * @return the current {@code BitFieldArgs}
     */
    public BitFieldArgs set(BitFieldType bft, int offset, long value) {
        return set(bft, new Offset(false, offset), value);
    }

    /**
     * Adds a new {@code SET} subcommand.
     *
     * @param bft
     *        the bit field type, must not be {@code null}.
     * @param offset
     *        bitfield offset, must not be {@code null}.
     * @param value
     *        the value
     *
     * @return the current {@code BitFieldArgs}
     */
    public BitFieldArgs set(BitFieldType bft, Offset offset, long value) {
        if (bft == null) {
            throw new IllegalArgumentException("The BitFieldType must not be `null`");
        }
        if (offset.offset < 0) {
            throw new IllegalArgumentException("The offset must be greater or equals to 0");
        }

        this.previousBitFieldType = bft;
        this.commands.addAll(List.of("SET", bft.toString(), offset.toString(), Long.toString(value)));
        return this;
    }

    /**
     * Adds a new {@code SET} subcommand using offset {@code 0} and the field type of the previous command.
     *
     * @param value
     *        the value
     *
     * @return the current {@code BitFieldArgs}
     *
     * @throws IllegalStateException
     *         if no previous field type was found
     */
    public BitFieldArgs set(long value) {
        return set(getPreviousFieldType(), value);
    }

    /**
     * Adds a new {@code SET} subcommand using offset {@code 0}.
     *
     * @param bitFieldType
     *        the bit field type, must not be {@code null}.
     * @param value
     *        the value
     *
     * @return the current {@code BitFieldArgs}
     */
    public BitFieldArgs set(BitFieldType bitFieldType, long value) {
        return set(bitFieldType, 0, value);
    }

    /**
     * Adds a new {@code SET} subcommand using the field type of the previous command.
     *
     * @param offset
     *        bitfield offset
     * @param value
     *        the value
     *
     * @return the current {@code BitFieldArgs}
     *
     * @throws IllegalStateException
     *         if no previous field type was found
     */
    public BitFieldArgs set(int offset, long value) {
        return set(getPreviousFieldType(), offset, value);
    }

    /**
     * Adds a new {@code INCRBY} subcommand.
     *
     * @param bitFieldType
     *        the bit field type, must not be {@code null}.
     * @param offset
     *        bitfield offset
     * @param value
     *        the value
     *
     * @return the current {@code BitFieldArgs}
     */
    public BitFieldArgs incrBy(BitFieldType bitFieldType, int offset, long value) {
        return incrBy(bitFieldType, new Offset(false, offset), value);
    }

    /**
     * Adds a new {@code INCRBY} subcommand.
     *
     * @param bft
     *        the bit field type, must not be {@code null}.
     * @param offset
     *        bitfield offset, must not be {@code null}.
     * @param value
     *        the value
     *
     * @return the current {@code BitFieldArgs}
     */
    public BitFieldArgs incrBy(BitFieldType bft, Offset offset, long value) {
        if (bft == null) {
            throw new IllegalArgumentException("`btf` must not be `null`");
        }
        if (offset == null) {
            throw new IllegalArgumentException("`offset` must not be `null`");
        }
        this.previousBitFieldType = bft;
        this.commands.addAll(List.of("INCRBY", bft.toString(), offset.toString(), Long.toString(value)));
        return this;
    }

    /**
     * Adds a new {@code INCRBY} subcommand using the field type of the previous command.
     *
     * @param offset
     *        bitfield offset
     * @param value
     *        the value
     *
     * @return a new {@code INCRBY} subcommand for the given {@code bitFieldType}, {@code offset} and {@code value}.
     *
     * @throws IllegalStateException
     *         if no previous field type was found
     */
    public BitFieldArgs incrBy(int offset, long value) {
        return incrBy(getPreviousFieldType(), offset, value);
    }

    /**
     * Adds a new {@code INCRBY} subcommand using offset {@code 0} and the field type of the previous command.
     *
     * @param value
     *        the value
     *
     * @return the current {@code BitFieldArgs}
     *
     * @throws IllegalStateException
     *         if no previous field type was found
     */
    public BitFieldArgs incrBy(long value) {
        return incrBy(getPreviousFieldType(), value);
    }

    /**
     * Adds a new {@code INCRBY} subcommand using offset {@code 0}.
     *
     * @param bitFieldType
     *        the bit field type, must not be {@code null}.
     * @param value
     *        the value
     *
     * @return the current {@code BitFieldArgs}
     */
    public BitFieldArgs incrBy(BitFieldType bitFieldType, long value) {
        return incrBy(bitFieldType, 0, value);
    }

    /**
     * Adds a new {@code OVERFLOW} subcommand.
     *
     * @param overflowType
     *        type of overflow, must not be {@code null}.
     *
     * @return the current {@code BitFieldArgs}
     */
    public BitFieldArgs overflow(OverflowType overflowType) {
        if (overflowType == null) {
            throw new IllegalArgumentException("`overflowType` must not be `null`");
        }
        this.commands.addAll(List.of("OVERFLOW", overflowType.name()));
        return this;
    }

    /**
     * Creates a new signed {@link BitFieldType} for the given number of {@code bits}. Redis allows up to {@code 64}
     * bits for unsigned integers.
     *
     * @param bits
     *        number of bits to define the integer type width.
     *
     * @return the {@link BitFieldType}.
     */
    public static BitFieldType signed(int bits) {
        return new BitFieldType(true, bits);
    }

    /**
     * Creates a new unsigned {@link BitFieldType} for the given number of {@code bits}. Redis allows up to {@code 63}
     * bits for unsigned integers.
     *
     * @param bits
     *        number of bits to define the integer type width.
     *
     * @return the {@link BitFieldType}.
     */
    public static BitFieldType unsigned(int bits) {
        return new BitFieldType(false, bits);
    }

    /**
     * Creates a new {@link Offset} for the given {@code offset}.
     *
     * @param offset
     *        zero-based offset.
     *
     * @return the {@link Offset}.
     */
    public static Offset offset(int offset) {
        return new Offset(false, offset);
    }

    /**
     * Creates a new {@link Offset} for the given {@code offset} that is multiplied by the integer type width used in
     * the sub command.
     *
     * @param offset
     *        offset to be multiplied by the integer type width.
     *
     * @return the {@link Offset}.
     */
    public static Offset typeWidthBasedOffset(int offset) {
        return new Offset(true, offset);
    }

    private BitFieldType getPreviousFieldType() {
        if (previousBitFieldType == null) {
            throw new IllegalStateException("No previous field type found");
        } else {
            return previousBitFieldType;
        }
    }

    public List<Object> toArgs() {
        return commands;
    }

}
