package io.quarkus.runtime.configuration;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.List;

import io.smallrye.common.constraint.Assert;

/**
 * A type representing data sizes.
 * This is a signed 128-bit integer value.
 */
public final class MemorySize implements Comparable<MemorySize> {
    /**
     * The memory size representing zero bytes.
     */
    public static final MemorySize ZERO = new MemorySize(0, 0);
    /**
     * The memory size representing negative one ({@code -1}).
     */
    public static final MemorySize MINUS_1 = new MemorySize(-1, -1);
    /**
     * The high 64 bits of this 128-bit signed integer value.
     */
    private final long high;
    /**
     * The low 64 bits of this 128-bit signed integer value.
     */
    private final long low;

    /**
     * Construct a new instance from high and low 64-bit words.
     * The value {@code (Long.MIN_VALUE, 0)} is explicitly disallowed because
     * negating it would overflow.
     *
     * @param high the high 64 bits
     * @param low the low 64 bits
     * @throws ArithmeticException if the given values equal {@code (Long.MIN_VALUE, 0)}
     */
    private MemorySize(final long high, final long low) {
        if (high == Long.MIN_VALUE && low == 0) {
            throw integerOverflow();
        }
        this.high = high;
        this.low = low;
    }

    /**
     * Construct a new instance from a {@link BigInteger}.
     *
     * @param value the value (must not be {@code null})
     * @deprecated Use one of the {@code of(*)} methods instead.
     */
    @Deprecated(forRemoval = true)
    public MemorySize(BigInteger value) {
        this(value.shiftRight(64).longValueExact(), value.longValue());
    }

    /**
     * {@return the memory size, in bytes}
     */
    public long asLongValue() {
        if (compareTo(Long.MIN_VALUE) < 0 || compareTo(Long.MAX_VALUE) > 0) {
            throw outOfLongRange();
        }
        return low;
    }

    /**
     * {@return the memory size, in bytes}
     */
    public int asIntValue() {
        if (compareTo(Integer.MIN_VALUE) < 0 || compareTo(Integer.MAX_VALUE) > 0) {
            throw outOfLongRange();
        }
        return (int) low;
    }

    /**
     * A {@link VarHandle} for reading/writing {@code long} values from/to a big-endian byte array.
     */
    private static final VarHandle BAH = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);

    /**
     * {@return the memory size value as a big integer (not {@code null})}
     */
    public BigInteger asBigInteger() {
        if (isZero()) {
            return BigInteger.ZERO;
        }
        byte[] b = new byte[16];
        BAH.set(b, 0, high);
        BAH.set(b, 8, low);
        return new BigInteger(b);
    }

    /**
     * Add two memory sizes.
     *
     * @param a the first addend (must not be {@code null})
     * @param b the second addend (must not be {@code null})
     * @return the sum (not {@code null})
     * @throws ArithmeticException if the result overflows 128 bits
     */
    public static MemorySize add(MemorySize a, MemorySize b) {
        long l = a.low + b.low;
        long h = addHigh(a.high, a.low, b.high, b.low);
        // signed overflow: both inputs same sign, result sign differs
        if (((a.high ^ h) & (b.high ^ h)) < 0) {
            throw integerOverflow();
        } else if (h == a.high && l == a.low) {
            return a;
        } else if (h == b.high && l == b.low) {
            return b;
        } else {
            return MemorySize.of(h, l);
        }
    }

    /**
     * Subtract one memory size from another.
     *
     * @param a the minuend (must not be {@code null})
     * @param b the subtrahend (must not be {@code null})
     * @return the difference (not {@code null})
     * @throws ArithmeticException if the result overflows 128 bits
     */
    public static MemorySize sub(MemorySize a, MemorySize b) {
        long l = a.low - b.low;
        long h = subHigh(a.high, a.low, b.high, b.low);
        // signed overflow: inputs have different signs and result sign differs from minuend
        if (((a.high ^ b.high) & (a.high ^ h)) < 0) {
            throw integerOverflow();
        } else if (h == a.high && l == a.low) {
            return a;
        } else if (h == b.high && l == b.low) {
            return b;
        } else {
            return MemorySize.of(h, l);
        }
    }

    /**
     * Left-shift a value by the given distance.
     * The shift saturates: distances of 128 or more yield {@link #ZERO}.
     * Negative distances reverse the direction (equivalent to {@link #shr(MemorySize, int) shr(a, -distance)}).
     *
     * @param a the value to shift (must not be {@code null})
     * @param distance the shift distance
     * @return the shifted value (not {@code null})
     */
    public static MemorySize shl(MemorySize a, int distance) {
        if (distance == 0 || a.equals(ZERO)) {
            return a;
        } else if (distance == Integer.MIN_VALUE) {
            return a.isNegative() ? MINUS_1 : ZERO;
        } else if (distance < 0) {
            return shr(a, -distance);
        } else {
            return of(satShlHi(a.high, a.low, distance), satShlLo(a.low, distance));
        }
    }

    /**
     * Arithmetic right-shift a value by the given distance.
     * The shift saturates: distances of 128 or more yield all-sign-bits (zero or minus one).
     * Negative distances reverse the direction (equivalent to {@link #shl(MemorySize, int) shl(a, -distance)}).
     *
     * @param a the value to shift (must not be {@code null})
     * @param distance the shift distance
     * @return the shifted value (not {@code null})
     */
    public static MemorySize shr(MemorySize a, int distance) {
        if (distance == 0 || a.equals(ZERO) || a.equals(MINUS_1)) {
            return a;
        } else if (distance == Integer.MIN_VALUE) {
            return ZERO;
        } else if (distance < 0) {
            return shl(a, -distance);
        } else {
            return of(satShrHi(a.high, distance), satShrLo(a.high, a.low, distance));
        }
    }

    /**
     * Negate this value.
     *
     * @return the negated value (not {@code null})
     */
    public MemorySize neg() {
        return of(-high - (Long.signum(low) & 1), -low);
    }

    /**
     * Multiply a memory size by a signed {@code long} multiplier.
     *
     * @param a the memory size (must not be {@code null})
     * @param b the multiplier
     * @return the product (not {@code null})
     * @throws ArithmeticException if the result overflows 128 bits
     */
    public static MemorySize mul(MemorySize a, long b) {
        if (b == 0 || a.isZero()) {
            return ZERO;
        } else if (b == 1) {
            return a;
        } else if (b == -1) {
            return a.neg();
        } else if (Long.bitCount(b) == 1 && b > 0) {
            int n = Long.numberOfTrailingZeros(b);
            if (a.isNegative()) {
                if (n >= a.numberOfLeadingOnes() - 1) {
                    throw integerOverflow();
                }
            } else {
                if (n >= a.numberOfLeadingZeros()) {
                    throw integerOverflow();
                }
            }
            return shl(a, n);
        } else {
            return MemorySize.of(a.asBigInteger().multiply(BigInteger.valueOf(b)));
        }
    }

    /**
     * Divide a memory size by a signed {@code long} divisor.
     * The result is truncated toward zero.
     *
     * @param a the dividend (must not be {@code null})
     * @param b the divisor
     * @return the quotient (not {@code null})
     * @throws ArithmeticException if the divisor is zero, or if the result overflows 128 bits
     */
    public static MemorySize div(MemorySize a, long b) {
        if (b == 0) {
            throw divisionByZero();
        } else if (a.equals(ZERO)) {
            return ZERO;
        } else if (b == 1) {
            return a;
        } else if (b == -1) {
            return a.neg();
        } else {
            return MemorySize.of(a.asBigInteger().divide(BigInteger.valueOf(b)));
        }
    }

    /**
     * Return the signum of this value: -1 if negative, 0 if zero, 1 if positive.
     *
     * @return the signum
     */
    public int signum() {
        return Long.signum(high) | Long.signum(low) & 1;
    }

    /**
     * {@return {@code true} if this value is zero}
     */
    public boolean isZero() {
        return (high | low) == 0;
    }

    /**
     * {@return {@code true} if this value is negative}
     */
    public boolean isNegative() {
        return high < 0;
    }

    /**
     * {@return {@code true} if this value is positive (greater than zero)}
     */
    public boolean isPositive() {
        return high > 0 || high == 0 && low != 0;
    }

    /**
     * Compare this value to an unsigned {@code long}.
     *
     * @param o the unsigned long value to compare to
     * @return the comparison result
     */
    public int compareToUnsigned(final long o) {
        return compare(high, low, 0, o);
    }

    /**
     * Compare this value to a {@code long}.
     *
     * @param o the long value to compare to
     * @return the comparison result
     */
    public int compareTo(final long o) {
        return compare(high, low, o >> 63, o);
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo(final MemorySize o) {
        return compare(high, low, o.high, o.low);
    }

    /**
     * {@return the number of trailing zero bits in this value, in the range of 0 to 128 (inclusive)}
     */
    public int numberOfTrailingZeros() {
        return numberOfTrailingZeros(high, low);
    }

    /**
     * {@return the number of trailing one bits in this value, in the range of 0 to 128 (inclusive)}
     */
    public int numberOfTrailingOnes() {
        return numberOfTrailingZeros(~high, ~low);
    }

    /**
     * {@return the number of leading zero bits in this value, in the range of 0 to 128 (inclusive)}
     */
    public int numberOfLeadingZeros() {
        return numberOfLeadingZeros(high, low);
    }

    /**
     * {@return the number of leading one bits in this value, in the range of 0 to 128 (inclusive)}
     */
    public int numberOfLeadingOnes() {
        return numberOfLeadingZeros(~high, ~low);
    }

    /**
     * {@return the natural scale of this value, which is the largest scale at which this value
     * is exactly representable as an integer}
     */
    public Scale scale() {
        int idx = Math.min(numberOfTrailingZeros() / 10, Scale.values.size() - 1);
        return Scale.values.get(idx);
    }

    /**
     * Align this value up (toward positive infinity) to a whole multiple of the given scale unit.
     * If this value is already a whole multiple, it is returned unchanged.
     *
     * @param scale the scale to align to (must not be {@code null})
     * @return the aligned value (not {@code null})
     * @throws ArithmeticException if the aligned value overflows 128 bits
     */
    public MemorySize alignUp(Scale scale) {
        return alignUpByShift(scale.shift());
    }

    /**
     * Align this value up (toward positive infinity) to a whole multiple of the given amount,
     * expressed as a power-of-two byte count.
     * If this value is already a whole multiple, it is returned unchanged.
     *
     * @param amount the alignment amount in bytes, which must be a positive power of two
     * @return the aligned value (not {@code null})
     * @throws IllegalArgumentException if {@code amount} is not a positive power of two
     * @throws ArithmeticException if the aligned value overflows 128 bits
     */
    public MemorySize alignUp(long amount) {
        if (amount <= 0 || Long.bitCount(amount) != 1) {
            throw notPositivePowerOfTwo();
        }
        return alignUpByShift(Long.numberOfTrailingZeros(amount));
    }

    /**
     * Align this value up (toward positive infinity) to a whole multiple of the given amount
     * scaled by the given scale unit.
     * The effective alignment is {@code amount * 2^scale.shift()} bytes.
     * If this value is already a whole multiple, it is returned unchanged.
     *
     * @param amount the alignment amount in scale units, which must be a positive power of two
     * @param scale the scale unit (must not be {@code null})
     * @return the aligned value (not {@code null})
     * @throws IllegalArgumentException if {@code amount} is not a positive power of two
     * @throws ArithmeticException if the aligned value overflows 128 bits
     */
    public MemorySize alignUp(long amount, Scale scale) {
        if (amount <= 0 || Long.bitCount(amount) != 1) {
            throw notPositivePowerOfTwo();
        }
        return alignUpByShift(Long.numberOfTrailingZeros(amount) + scale.shift());
    }

    /**
     * Align this value up by a power-of-two shift amount.
     *
     * @param shift the alignment shift (number of low bits to clear)
     * @return the aligned value (not {@code null})
     * @throws ArithmeticException if the aligned value overflows 128 bits
     */
    private MemorySize alignUpByShift(int shift) {
        if (shift == 0) {
            return this;
        }
        if (shift > 127) {
            throw integerOverflow();
        }
        // bias = (1 << shift) - 1 = (2^127 - 1) >>> (127 - shift)
        long bh = satShrHi(Long.MAX_VALUE, 127 - shift);
        long bl = satShrLo(Long.MAX_VALUE, -1L, 127 - shift);
        // add bias with carry
        long rl = low + bl;
        long rh = addHigh(high, low, bh, bl);
        // mask off the low bits
        rh &= ~bh;
        rl &= ~bl;
        if (rh == high && rl == low) {
            return this;
        } else {
            return of(rh, rl);
        }
    }

    /**
     * Return the lesser of two memory sizes.
     *
     * @param a the first value (must not be {@code null})
     * @param b the second value (must not be {@code null})
     * @return the lesser value (not {@code null})
     */
    public static MemorySize min(MemorySize a, MemorySize b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    /**
     * Return the greater of two memory sizes.
     *
     * @param a the first value (must not be {@code null})
     * @param b the second value (must not be {@code null})
     * @return the greater value (not {@code null})
     */
    public static MemorySize max(MemorySize a, MemorySize b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    /**
     * Get a {@code MemorySize} whose value is equal to the given signed long integer value.
     *
     * @param value the signed value
     * @return the {@code MemorySize} (not {@code null})
     */
    public static MemorySize of(long value) {
        return of(value >> 63, value);
    }

    /**
     * Get a {@code MemorySize} whose value is equal to the given unsigned long integer value.
     *
     * @param value the unsigned value
     * @return the {@code MemorySize} (not {@code null})
     */
    public static MemorySize ofUnsigned(long value) {
        return of(0, value);
    }

    /**
     * Get a {@code MemorySize} whose value is equal to the given unsigned integer value.
     *
     * @param value the unsigned value
     * @return the {@code MemorySize} (not {@code null})
     */
    public static MemorySize ofUnsigned(int value) {
        return of(0, Integer.toUnsignedLong(value));
    }

    /**
     * Get a {@code MemorySize} whose value is defined by the given high and low bit values.
     * Note that the values {@code (Long.MIN_VALUE, 0)} are explicitly disallowed.
     *
     * @param high the high bits
     * @param low the low bits
     * @return the {@code MemorySize} (not {@code null})
     * @throws ArithmeticException if the given values are equal to {@code (Long.MIN_VALUE, 0)}
     */
    public static MemorySize of(long high, long low) {
        return (high | low) == 0 ? ZERO : (high & low) == -1 ? MINUS_1 : new MemorySize(high, low);
    }

    /**
     * Get a {@code MemorySize} whose value is equal to the given {@code BigInteger} value.
     * If the given value falls outside the range of this type, an {@code ArithmeticException} is thrown.
     *
     * @param value the big integer value (must not be {@code null})
     * @return the {@code MemorySize} (not {@code null})
     * @throws ArithmeticException if the given value is out of range
     */
    public static MemorySize of(BigInteger value) {
        return of(value.shiftRight(64).longValueExact(), value.longValue());
    }

    /**
     * Parse a memory size string.
     * The format is {@code [-]<digits>[<suffix>]} where the suffix is one of
     * {@code B}, {@code K}, {@code M}, {@code G}, {@code T}, {@code P}, {@code E}, {@code Z}, {@code Y}
     * (case-insensitive). If no suffix is given, the value is treated as bytes.
     *
     * @param s the string to parse (must not be {@code null})
     * @return the parsed memory size (not {@code null})
     * @throws IllegalArgumentException if the string is not a valid memory size
     */
    public static MemorySize of(String s) {
        s = s.trim();
        if (s.isEmpty()) {
            throw parseEmpty();
        }
        int idx = 0;
        boolean neg = false;
        if (s.charAt(idx) == '-') {
            neg = true;
            idx++;
            if (idx >= s.length()) {
                throw parseNoDigits();
            }
        }
        long h = 0, l = 0;
        boolean hasDigits = false;
        while (idx < s.length()) {
            int cp = s.codePointAt(idx);
            int d = Character.digit(cp, 10);
            if (d == -1) {
                break;
            }
            hasDigits = true;
            // multiply (h, l) by 10 and add d
            // check h * 10 doesn't overflow 64 bits
            if (Math.multiplyHigh(h, 10L) != 0) {
                throw parseOverflow();
            }
            h = h * 10L + Math.unsignedMultiplyHigh(l, 10L);
            // add digit to low part
            long l10 = l * 10L;
            h = addHigh(h, l10, 0, d);
            l = l10 + d;
            // overflow: during accumulation the value is non-negative, so h must stay >= 0
            if (h < 0) {
                throw parseOverflow();
            }
            idx += Character.charCount(cp);
        }
        if (!hasDigits) {
            throw parseNoDigits();
        }
        // parse the scale suffix
        Scale scale;
        if (idx >= s.length()) {
            // no suffix means bytes
            scale = Scale.B;
        } else {
            int cp = s.codePointAt(idx);
            scale = Scale.forSuffix(cp);
            if (scale == null) {
                throw invalidSuffix(cp);
            }
            idx += Character.charCount(cp);
            if (idx < s.length()) {
                throw trailingContent(s.substring(idx));
            }
        }
        // apply scale by left-shifting; overflow if there aren't enough leading zeros to absorb the shift
        int shift = scale.shift();
        int nlz = numberOfLeadingZeros(h, l);
        if (shift >= nlz) {
            throw parseOverflow();
        }
        h = satShlHi(h, l, shift);
        l = satShlLo(l, shift);
        // apply negation using proper two's complement
        if (neg) {
            l = -l;
            h = l == 0 ? -h : ~h;
        }
        return MemorySize.of(h, l);
    }

    /**
     * Compute the high word of a saturated 128-bit left shift {@code (h, l) << amt}.
     * Saturates to zero for amounts of 128 or more.
     *
     * @param h the high word
     * @param l the low word
     * @param amt the shift amount (must be non-negative)
     * @return the high word of the shifted result
     */
    private static long satShlHi(long h, long l, int amt) {
        if (amt >= 128) {
            return 0;
        } else if (amt >= 64) {
            return l << amt - 64;
        } else if (amt == 0) {
            return h;
        } else {
            return h << amt | l >>> 64 - amt;
        }
    }

    /**
     * Compute the low word of a saturated 128-bit left shift {@code (*, l) << amt}.
     * Saturates to zero for amounts of 64 or more.
     *
     * @param l the low word
     * @param amt the shift amount (must be non-negative)
     * @return the low word of the shifted result
     */
    private static long satShlLo(long l, int amt) {
        if (amt >= 64) {
            return 0;
        } else {
            return l << amt;
        }
    }

    /**
     * Compute the high word of a saturated 128-bit arithmetic right shift {@code (h, *) >> amt}.
     * Saturates to the sign extension (all zeros or all ones) for amounts of 64 or more.
     *
     * @param h the high word
     * @param amt the shift amount (must be non-negative)
     * @return the high word of the shifted result
     */
    private static long satShrHi(long h, int amt) {
        if (amt >= 64) {
            return h >> 63;
        } else {
            return h >> amt;
        }
    }

    /**
     * Compute the low word of a saturated 128-bit arithmetic right shift {@code (h, l) >> amt}.
     * Saturates to the sign extension (all zeros or all ones) for amounts of 127 or more.
     *
     * @param h the high word
     * @param l the low word
     * @param amt the shift amount (must be non-negative)
     * @return the low word of the shifted result
     */
    private static long satShrLo(long h, long l, int amt) {
        if (amt >= 127) {
            return h >> 63;
        } else if (amt >= 64) {
            return h >> amt - 64;
        } else if (amt == 0) {
            return l;
        } else {
            return h << 64 - amt | l >>> amt;
        }
    }

    /**
     * Count the number of leading zero bits in a 128-bit value.
     *
     * @param h the high word
     * @param l the low word
     * @return the number of leading zeros, from 0 to 128 inclusive
     */
    private static int numberOfLeadingZeros(long h, long l) {
        int res = Long.numberOfLeadingZeros(h);
        if (res == 64) {
            res += Long.numberOfLeadingZeros(l);
        }
        return res;
    }

    /**
     * Count the number of trailing zero bits in a 128-bit value.
     *
     * @param h the high word
     * @param l the low word
     * @return the number of trailing zeros, from 0 to 128 inclusive
     */
    private static int numberOfTrailingZeros(long h, long l) {
        int res = Long.numberOfTrailingZeros(l);
        if (res == 64) {
            res += Long.numberOfTrailingZeros(h);
        }
        return res;
    }

    /**
     * Compare two signed 128-bit values represented as {@code (high, low)} pairs.
     *
     * @param ah the high word of the first value
     * @param al the low word of the first value
     * @param bh the high word of the second value
     * @param bl the low word of the second value
     * @return a negative value, zero, or a positive value as the first value is less than, equal to,
     *         or greater than the second
     */
    private static int compare(long ah, long al, long bh, long bl) {
        int res = Long.compare(ah, bh);
        if (res == 0) {
            res = Long.compareUnsigned(al, bl);
        }
        return Long.signum(res);
    }

    /**
     * Compute the high word of a 128-bit addition {@code (ah, al) + (bh, bl)}.
     * The caller computes the low word as {@code al + bl}.
     */
    private static long addHigh(long ah, long al, long bh, long bl) {
        return ah + bh + (Long.compareUnsigned(al + bl, al) >>> 31);
    }

    /**
     * Compute the high word of a 128-bit subtraction {@code (ah, al) - (bh, bl)}.
     * The caller computes the low word as {@code al - bl}.
     */
    private static long subHigh(long ah, long al, long bh, long bl) {
        return ah - bh + (Long.compareUnsigned(al, bl) >> 31);
    }

    /**
     * {@return a new exception indicating integer overflow}
     */
    private static ArithmeticException integerOverflow() {
        return new ArithmeticException("Integer overflow");
    }

    /**
     * {@return a new exception indicating division by zero}
     */
    private static ArithmeticException divisionByZero() {
        return new ArithmeticException("Division by zero");
    }

    /**
     * {@return a new exception indicating the value is out of the range of {@code long}}
     */
    private static ArithmeticException outOfLongRange() {
        return new ArithmeticException("Value is out of range of long");
    }

    /**
     * {@return a new exception indicating that a parsed value overflows the maximum memory size}
     */
    private static IllegalArgumentException parseOverflow() {
        return new IllegalArgumentException("Value overflows largest possible memory size");
    }

    /**
     * {@return a new exception indicating that the memory size string is empty}
     */
    private static IllegalArgumentException parseEmpty() {
        return new IllegalArgumentException("Empty memory size string");
    }

    /**
     * {@return a new exception indicating that the memory size string contains no digits}
     */
    private static IllegalArgumentException parseNoDigits() {
        return new IllegalArgumentException("No digits in memory size string");
    }

    /**
     * Create a new exception indicating an invalid scale suffix character.
     *
     * @param codePoint the invalid code point
     * @return the exception (not {@code null})
     */
    private static IllegalArgumentException invalidSuffix(int codePoint) {
        return new IllegalArgumentException("Invalid suffix character: " + new String(Character.toChars(codePoint)));
    }

    /**
     * Create a new exception indicating unexpected trailing content in a memory size string.
     *
     * @param content the trailing content
     * @return the exception (not {@code null})
     */
    private static IllegalArgumentException trailingContent(String content) {
        return new IllegalArgumentException("Unexpected trailing content: " + content);
    }

    /**
     * {@return a new exception indicating that an alignment amount is not a positive power of two}
     */
    private static IllegalArgumentException notPositivePowerOfTwo() {
        return new IllegalArgumentException("Alignment amount must be a positive power of two");
    }

    /**
     * Return a string representation of this memory size.
     * The format is {@code [-]<digits>[<suffix>]} where the suffix is the largest
     * scale at which this value is exactly representable.
     * Zero is formatted as {@code "0"}.
     * The suffix character is printed in uppercase.
     *
     * @return the string representation
     */
    public String toString() {
        return toStringImpl(null, false);
    }

    /**
     * Return a string representation of this memory size.
     * The format is {@code [-]<digits>[<suffix>]} where the suffix is the given scale.
     * Zero is formatted as {@code "0"}.
     * The value is rounded up if it is not exactly representable.
     * The suffix character is printed in uppercase.
     *
     * @param scale the scale to use (must not be {@code null})
     * @return the string representation
     */
    public String toString(Scale scale) {
        return toStringImpl(Assert.checkNotNullParam("scale", scale), false);
    }

    /**
     * Return a string representation of this memory size.
     * The format is {@code [-]<digits>[<suffix>]} where the suffix is the largest
     * scale at which this value is exactly representable.
     * Zero is formatted as {@code "0"}.
     * The suffix character is printed in lowercase.
     *
     * @return the string representation
     */
    public String toLowerString() {
        return toStringImpl(null, true);
    }

    /**
     * Return a string representation of this memory size.
     * The format is {@code [-]<digits>[<suffix>]} where the suffix is the given scale.
     * Zero is formatted as {@code "0"}.
     * The value is rounded up if it is not exactly representable.
     * The suffix character is printed in lowercase.
     *
     * @param scale the scale to use (must not be {@code null})
     * @return the string representation
     */
    public String toLowerString(Scale scale) {
        return toStringImpl(Assert.checkNotNullParam("scale", scale), true);
    }

    /**
     * Produce the string representation of this memory size at the given scale.
     * If {@code scale} is {@code null}, the natural scale is used.
     *
     * @param scale the scale to format at, or {@code null} for the natural scale
     * @param lower {@code true} for lowercase suffix, or {@code false} for uppercase
     * @return the string representation (not {@code null})
     */
    private String toStringImpl(Scale scale, boolean lower) {
        if (high == 0 && low == 0) {
            return "0";
        }
        boolean neg = high < 0;
        long ah, al;
        if (neg) {
            al = -low;
            ah = low == 0 ? -high : ~high;
        } else {
            ah = high;
            al = low;
        }
        if (scale == null) {
            scale = Scale.values.get(Math.min(numberOfTrailingZeros() / 10, Scale.values.size() - 1));
        }
        int shift = scale.shift();
        // round the magnitude up when not exactly representable at the given scale
        // bias = (1 << shift) - 1 = (2^127 - 1) >>> (127 - shift)
        long rh = satShrHi(Long.MAX_VALUE, 127 - shift);
        long rl = satShrLo(Long.MAX_VALUE, -1L, 127 - shift);
        ah = addHigh(ah, al, rh, rl);
        al += rl;
        // right-shift the absolute value by the scale shift (ah is non-negative, so arithmetic == logical)
        long ch = satShrHi(ah, shift);
        long cl = satShrLo(ah, al, shift);
        // convert (ch, cl) to decimal using 32-bit schoolbook division
        StringBuilder sb = new StringBuilder(41);
        while (ch != 0 || cl != 0) {
            // divide (ch, cl) by 10 using 32-bit words
            long rem;
            long combined;

            combined = Integer.toUnsignedLong((int) (ch >>> 32));
            long q3 = Long.divideUnsigned(combined, 10);
            rem = Long.remainderUnsigned(combined, 10);

            combined = rem << 32 | Integer.toUnsignedLong((int) ch);
            long q2 = Long.divideUnsigned(combined, 10);
            rem = Long.remainderUnsigned(combined, 10);

            combined = rem << 32 | Integer.toUnsignedLong((int) (cl >>> 32));
            long q1 = Long.divideUnsigned(combined, 10);
            rem = Long.remainderUnsigned(combined, 10);

            combined = rem << 32 | Integer.toUnsignedLong((int) cl);
            long q0 = Long.divideUnsigned(combined, 10);
            rem = Long.remainderUnsigned(combined, 10);

            sb.append((char) ('0' + rem));
            ch = q3 << 32 | q2;
            cl = q1 << 32 | q0;
        }
        if (neg) {
            sb.append('-');
        }
        sb.reverse();
        if (scale != Scale.B) {
            sb.append(lower ? scale.asLowerChar() : scale.asChar());
        }
        return sb.toString();
    }

    /**
     * The scale values for a memory size.
     */
    public enum Scale {
        // NOTE: These must be in strictly increasing order, in increments of 2**10 (1024), with no gaps.
        /** Bytes (2<sup>0</sup> = 1 byte). */
        B,
        /** Kibibytes (2<sup>10</sup> = 1024 bytes). */
        K,
        /** Mebibytes (2<sup>20</sup> = 1048576 bytes). */
        M,
        /** Gibibytes (2<sup>30</sup> bytes). */
        G,
        /** Tebibytes (2<sup>40</sup> bytes). */
        T,
        /** Pebibytes (2<sup>50</sup> bytes). */
        P,
        /** Exbibytes (2<sup>60</sup> bytes). */
        E,
        /** Zebibytes (2<sup>70</sup> bytes). */
        Z,
        /** Yobibytes (2<sup>80</sup> bytes). */
        Y,
        ;

        /**
         * An immutable list of all scale values.
         */
        public static final List<Scale> values = List.of(values());

        /**
         * Construct a new scale constant.
         */
        Scale() {
        }

        /**
         * {@return the number of trailing bits corresponding to the scale value}
         */
        public int shift() {
            return ordinal() * 10;
        }

        /**
         * {@return the (uppercase) suffix character of this unit}
         */
        public char asChar() {
            return name().charAt(0);
        }

        /**
         * {@return the (lowercase) suffix character of this unit}
         */
        public char asLowerChar() {
            return Character.toLowerCase(asChar());
        }

        /**
         * Get the scale corresponding to a suffix character code point.
         *
         * @param codePoint the code point of the suffix character (case-insensitive)
         * @return the matching scale, or {@code null} if the code point is not a valid suffix
         */
        public static Scale forSuffix(int codePoint) {
            return switch (codePoint) {
                case 'b', 'B' -> B;
                case 'k', 'K' -> K;
                case 'm', 'M' -> M;
                case 'g', 'G' -> G;
                case 't', 'T' -> T;
                case 'p', 'P' -> P;
                case 'e', 'E' -> E;
                case 'z', 'Z' -> Z;
                case 'y', 'Y' -> Y;
                default -> null;
            };
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof MemorySize other && equals(other);
    }

    /**
     * Determine if this memory size is equal to another.
     *
     * @param other the other memory size (must not be {@code null})
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(MemorySize other) {
        return this == other || other != null && high == other.high && low == other.low;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Long.hashCode(high) * 31 + Long.hashCode(low);
    }
}
