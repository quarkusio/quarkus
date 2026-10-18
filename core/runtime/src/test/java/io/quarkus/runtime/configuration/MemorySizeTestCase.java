package io.quarkus.runtime.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MemorySize}.
 */
public class MemorySizeTestCase {

    private static final BigInteger KILO = BigInteger.valueOf(1024);

    // ── of() parsing tests (adapted from MemorySizeConverterTestCase) ──

    @Test
    public void testOfInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> MemorySize.of("HJ"));
    }

    @Test
    public void testOfEmpty() {
        assertThrows(IllegalArgumentException.class, () -> MemorySize.of(""));
    }

    @Test
    public void testOfBareMinus() {
        assertThrows(IllegalArgumentException.class, () -> MemorySize.of("-"));
    }

    @Test
    public void testOfNoSuffixTreatedAsBytes() {
        assertEquals(100L, MemorySize.of("100").asLongValue());
    }

    @Test
    public void testOfBytes() {
        assertEquals(100L, MemorySize.of("100B").asLongValue());
        assertEquals(100L, MemorySize.of("100b").asLongValue());
    }

    @Test
    public void testOfKiloBytes() {
        long expected = KILO.longValue() * 100L;
        assertEquals(expected, MemorySize.of("100K").asLongValue());
        assertEquals(expected, MemorySize.of("100k").asLongValue());
    }

    @Test
    public void testOfMegaBytes() {
        long expected = KILO.pow(2).longValue() * 100L;
        assertEquals(expected, MemorySize.of("100M").asLongValue());
        assertEquals(expected, MemorySize.of("100m").asLongValue());
    }

    @Test
    public void testOfGigaBytes() {
        long expected = KILO.pow(3).longValue() * 27L;
        assertEquals(expected, MemorySize.of("27G").asLongValue());
        assertEquals(expected, MemorySize.of("27g").asLongValue());
    }

    @Test
    public void testOfTeraBytes() {
        long expected = KILO.pow(4).longValue() * 19L;
        assertEquals(expected, MemorySize.of("19T").asLongValue());
        assertEquals(expected, MemorySize.of("19t").asLongValue());
    }

    @Test
    public void testOfPetaBytes() {
        long expected = KILO.pow(5).longValue() * 31L;
        assertEquals(expected, MemorySize.of("31P").asLongValue());
        assertEquals(expected, MemorySize.of("31p").asLongValue());
    }

    @Test
    public void testOfExaBytes() {
        BigInteger expected = KILO.pow(6).multiply(BigInteger.valueOf(9));
        assertEquals(expected, MemorySize.of("9E").asBigInteger());
        assertEquals(expected, MemorySize.of("9e").asBigInteger());
    }

    @Test
    public void testOfZettaBytes() {
        BigInteger expected = KILO.pow(7).multiply(BigInteger.valueOf(5));
        assertEquals(expected, MemorySize.of("5Z").asBigInteger());
        assertEquals(expected, MemorySize.of("5z").asBigInteger());
    }

    @Test
    public void testOfYottaBytes() {
        BigInteger expected = KILO.pow(8).multiply(BigInteger.valueOf(23));
        assertEquals(expected, MemorySize.of("23Y").asBigInteger());
        assertEquals(expected, MemorySize.of("23y").asBigInteger());
    }

    @Test
    public void testOfWhitespaceTrimmed() {
        assertEquals(100L, MemorySize.of("  100B  ").asLongValue());
    }

    @Test
    public void testOfTrailingContent() {
        assertThrows(IllegalArgumentException.class, () -> MemorySize.of("100BX"));
    }

    @Test
    public void testOfOverflow() {
        assertThrows(IllegalArgumentException.class,
                () -> MemorySize.of("999999999999999999999999999999999999999Y"));
    }

    // ── Constructor and conversion tests ──

    @Test
    public void testLongConstructor() {
        assertEquals(42L, MemorySize.of(42L).asLongValue());
        assertEquals(-1L, MemorySize.of(-1L).asLongValue());
        assertEquals(0L, MemorySize.of(0L).asLongValue());
    }

    @Test
    public void testBigIntegerConstructor() {
        BigInteger big = KILO.pow(7);
        assertEquals(big, MemorySize.of(big).asBigInteger());
    }

    @Test
    public void testBigIntegerRoundTrip() {
        BigInteger val = KILO.pow(8).multiply(BigInteger.valueOf(23));
        MemorySize ms = MemorySize.of(val);
        assertEquals(val, ms.asBigInteger());
    }

    @Test
    public void testBigIntegerNegativeRoundTrip() {
        BigInteger val = BigInteger.valueOf(-12345);
        assertEquals(val, MemorySize.of(val).asBigInteger());
    }

    @Test
    public void testAsLongValueOutOfRange() {
        MemorySize large = MemorySize.of("9E");
        assertThrows(ArithmeticException.class, large::asLongValue);
    }

    // ── of(long) factory method tests ──

    @Test
    public void testOfLongZero() {
        assertSame(MemorySize.ZERO, MemorySize.of(0L));
    }

    @Test
    public void testOfLongMinusOne() {
        assertSame(MemorySize.MINUS_1, MemorySize.of(-1L));
    }

    @Test
    public void testOfLongMaxValue() {
        MemorySize ms = MemorySize.of(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, ms.asLongValue());
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE), ms.asBigInteger());
    }

    @Test
    public void testOfLongMinValue() {
        MemorySize ms = MemorySize.of(Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, ms.asLongValue());
        assertEquals(BigInteger.valueOf(Long.MIN_VALUE), ms.asBigInteger());
    }

    @Test
    public void testOfLongPositive() {
        assertEquals(BigInteger.valueOf(1000), MemorySize.of(1000L).asBigInteger());
    }

    @Test
    public void testOfLongNegative() {
        assertEquals(BigInteger.valueOf(-42), MemorySize.of(-42L).asBigInteger());
    }

    // ── of(long, long) factory method tests ──

    @Test
    public void testOfHighLowZero() {
        assertSame(MemorySize.ZERO, MemorySize.of(0L, 0L));
    }

    @Test
    public void testOfHighLowMinusOne() {
        assertSame(MemorySize.MINUS_1, MemorySize.of(-1L, -1L));
    }

    @Test
    public void testOfHighLowRejectsMin128() {
        assertThrows(ArithmeticException.class, () -> MemorySize.of(Long.MIN_VALUE, 0L));
    }

    @Test
    public void testOfHighLowPositive() {
        // value = 1 << 64 = 2^64
        MemorySize ms = MemorySize.of(1L, 0L);
        assertEquals(BigInteger.ONE.shiftLeft(64), ms.asBigInteger());
    }

    @Test
    public void testOfHighLowNegative() {
        // value = -2 (high=-1, low=-2)
        MemorySize ms = MemorySize.of(-1L, -2L);
        assertEquals(BigInteger.valueOf(-2), ms.asBigInteger());
    }

    @Test
    public void testOfHighLowMaxValue() {
        // MAX_128 = 2^127 - 1 = (Long.MAX_VALUE, -1)
        MemorySize ms = MemorySize.of(Long.MAX_VALUE, -1L);
        assertEquals(MAX_128, ms.asBigInteger());
    }

    @Test
    public void testOfHighLowMinAllowed() {
        // min allowed = -(2^127 - 1) = (Long.MIN_VALUE, 1)
        MemorySize ms = MemorySize.of(Long.MIN_VALUE, 1L);
        assertEquals(MAX_128.negate(), ms.asBigInteger());
    }

    @Test
    public void testOfHighLowLowBitsOnly() {
        // high = 0, low = some arbitrary value
        MemorySize ms = MemorySize.of(0L, 0xDEAD_BEEF_CAFE_BABEL);
        assertEquals(Long.toUnsignedString(0xDEAD_BEEF_CAFE_BABEL),
                ms.asBigInteger().toString());
    }

    // ── ofUnsigned(long) factory method tests ──

    @Test
    public void testOfUnsignedLongZero() {
        assertSame(MemorySize.ZERO, MemorySize.ofUnsigned(0L));
    }

    @Test
    public void testOfUnsignedLongPositive() {
        MemorySize ms = MemorySize.ofUnsigned(42L);
        assertEquals(BigInteger.valueOf(42), ms.asBigInteger());
    }

    @Test
    public void testOfUnsignedLongMaxSignedValue() {
        MemorySize ms = MemorySize.ofUnsigned(Long.MAX_VALUE);
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE), ms.asBigInteger());
    }

    @Test
    public void testOfUnsignedLongNegativeBitsInterpretedAsUnsigned() {
        // -1L as unsigned = 2^64 - 1
        MemorySize ms = MemorySize.ofUnsigned(-1L);
        BigInteger expected = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);
        assertEquals(expected, ms.asBigInteger());
        assertTrue(ms.asBigInteger().signum() > 0);
    }

    @Test
    public void testOfUnsignedLongMinValueInterpretedAsUnsigned() {
        // Long.MIN_VALUE as unsigned = 2^63
        MemorySize ms = MemorySize.ofUnsigned(Long.MIN_VALUE);
        assertEquals(BigInteger.ONE.shiftLeft(63), ms.asBigInteger());
    }

    // ── ofUnsigned(int) factory method tests ──

    @Test
    public void testOfUnsignedIntZero() {
        assertSame(MemorySize.ZERO, MemorySize.ofUnsigned(0));
    }

    @Test
    public void testOfUnsignedIntPositive() {
        assertEquals(BigInteger.valueOf(1000), MemorySize.ofUnsigned(1000).asBigInteger());
    }

    @Test
    public void testOfUnsignedIntMaxValue() {
        // Integer.MAX_VALUE = 2^31 - 1
        assertEquals(BigInteger.valueOf(Integer.MAX_VALUE), MemorySize.ofUnsigned(Integer.MAX_VALUE).asBigInteger());
    }

    @Test
    public void testOfUnsignedIntNegativeBitsInterpretedAsUnsigned() {
        // -1 as unsigned int = 2^32 - 1 = 4294967295
        MemorySize ms = MemorySize.ofUnsigned(-1);
        assertEquals(BigInteger.valueOf(0xFFFF_FFFFL), ms.asBigInteger());
    }

    @Test
    public void testOfUnsignedIntMinValueInterpretedAsUnsigned() {
        // Integer.MIN_VALUE as unsigned = 2^31 = 2147483648
        MemorySize ms = MemorySize.ofUnsigned(Integer.MIN_VALUE);
        assertEquals(BigInteger.ONE.shiftLeft(31), ms.asBigInteger());
    }

    // ── Arithmetic tests ──

    @Test
    public void testAdd() {
        MemorySize a = MemorySize.of("100K");
        MemorySize b = MemorySize.of("200K");
        assertEquals(MemorySize.of("300K"), MemorySize.add(a, b));
    }

    @Test
    public void testAddZeroReturnsSame() {
        MemorySize a = MemorySize.of("100K");
        assertSame(a, MemorySize.add(a, MemorySize.ZERO));
    }

    @Test
    public void testAddCarry() {
        // values that cause unsigned carry in the low 64 bits
        MemorySize a = MemorySize.of(Long.MAX_VALUE);
        MemorySize b = MemorySize.of(1L);
        BigInteger expected = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
        assertEquals(expected, MemorySize.add(a, b).asBigInteger());
    }

    @Test
    public void testSub() {
        MemorySize a = MemorySize.of("300K");
        MemorySize b = MemorySize.of("100K");
        assertEquals(MemorySize.of("200K"), MemorySize.sub(a, b));
    }

    @Test
    public void testSubZeroReturnsSame() {
        MemorySize a = MemorySize.of("100K");
        assertSame(a, MemorySize.sub(a, MemorySize.ZERO));
    }

    @Test
    public void testSubBorrow() {
        MemorySize a = MemorySize.of(0L);
        MemorySize b = MemorySize.of(1L);
        assertEquals(BigInteger.valueOf(-1), MemorySize.sub(a, b).asBigInteger());
    }

    @Test
    public void testNeg() {
        MemorySize pos = MemorySize.of("100K");
        MemorySize negated = pos.neg();
        assertEquals(MemorySize.ZERO, MemorySize.add(pos, negated));
    }

    @Test
    public void testNegZero() {
        assertSame(MemorySize.ZERO, MemorySize.ZERO.neg());
    }

    // ── Shift tests ──

    @Test
    public void testShlZero() {
        MemorySize ms = MemorySize.of("1K");
        assertSame(ms, MemorySize.shl(ms, 0));
    }

    @Test
    public void testShlBasic() {
        MemorySize one = MemorySize.of(1L);
        assertEquals(MemorySize.of("1K"), MemorySize.shl(one, 10));
    }

    @Test
    public void testShlExactly64() {
        MemorySize one = MemorySize.of(1L);
        BigInteger expected = BigInteger.ONE.shiftLeft(64);
        assertEquals(expected, MemorySize.shl(one, 64).asBigInteger());
    }

    @Test
    public void testShlOver64() {
        MemorySize one = MemorySize.of(1L);
        BigInteger expected = BigInteger.ONE.shiftLeft(70);
        assertEquals(expected, MemorySize.shl(one, 70).asBigInteger());
    }

    @Test
    public void testShlSaturates() {
        MemorySize ms = MemorySize.of("1K");
        assertSame(MemorySize.ZERO, MemorySize.shl(ms, 128));
        assertSame(MemorySize.ZERO, MemorySize.shl(ms, 200));
    }

    @Test
    public void testShlNegativeReversesDirection() {
        MemorySize ms = MemorySize.of("1M");
        assertEquals(MemorySize.of("1K"), MemorySize.shl(ms, -10));
    }

    @Test
    public void testShrZero() {
        MemorySize ms = MemorySize.of("1K");
        assertSame(ms, MemorySize.shr(ms, 0));
    }

    @Test
    public void testShrBasic() {
        assertEquals(MemorySize.of(1L), MemorySize.shr(MemorySize.of("1K"), 10));
    }

    @Test
    public void testShrExactly64() {
        MemorySize large = MemorySize.of(BigInteger.ONE.shiftLeft(64));
        assertEquals(MemorySize.of(1L), MemorySize.shr(large, 64));
    }

    @Test
    public void testShrOver64() {
        MemorySize large = MemorySize.of(BigInteger.ONE.shiftLeft(70));
        assertEquals(MemorySize.of(1L), MemorySize.shr(large, 70));
    }

    @Test
    public void testShrSaturatesPositive() {
        MemorySize ms = MemorySize.of("1K");
        assertEquals(MemorySize.ZERO, MemorySize.shr(ms, 128));
    }

    @Test
    public void testShrSaturatesNegative() {
        MemorySize neg = MemorySize.of(-1L);
        assertEquals(MemorySize.of(-1L), MemorySize.shr(neg, 128));
    }

    @Test
    public void testShrNegativeReversesDirection() {
        MemorySize ms = MemorySize.of("1K");
        assertEquals(MemorySize.of("1M"), MemorySize.shr(ms, -10));
    }

    // ── Shift boundary tests (exercise satShl*/satShr* helper transitions) ──

    @Test
    public void testShlBy63() {
        BigInteger expected = BigInteger.ONE.shiftLeft(63);
        assertEquals(expected, MemorySize.shl(MemorySize.of(1L), 63).asBigInteger());
    }

    @Test
    public void testShlBy65() {
        BigInteger expected = BigInteger.ONE.shiftLeft(65);
        assertEquals(expected, MemorySize.shl(MemorySize.of(1L), 65).asBigInteger());
    }

    @Test
    public void testShlBy127() {
        BigInteger expected = BigInteger.ONE.shiftLeft(127);
        // 1 << 127 = 2^127, which exceeds MAX_128 (2^127 - 1), so the constructor throws
        assertThrows(ArithmeticException.class, () -> MemorySize.shl(MemorySize.of(1L), 127));
    }

    @Test
    public void testShlBy126() {
        BigInteger expected = BigInteger.ONE.shiftLeft(126);
        assertEquals(expected, MemorySize.shl(MemorySize.of(1L), 126).asBigInteger());
    }

    @Test
    public void testShlCrossWordValue() {
        // value with bits in both words: 2^32 + 1
        BigInteger val = BigInteger.ONE.shiftLeft(32).add(BigInteger.ONE);
        BigInteger expected = val.shiftLeft(40);
        assertEquals(expected, MemorySize.shl(MemorySize.of(val), 40).asBigInteger());
    }

    @Test
    public void testShlCrossWordValueBy64() {
        // value with low bits, shifted entirely into high word
        BigInteger val = BigInteger.valueOf(0xABCD_1234L);
        BigInteger expected = val.shiftLeft(64);
        assertEquals(expected, MemorySize.shl(MemorySize.of(val), 64).asBigInteger());
    }

    @Test
    public void testShrBy63() {
        BigInteger val = BigInteger.ONE.shiftLeft(63);
        assertEquals(BigInteger.ONE, MemorySize.shr(MemorySize.of(val), 63).asBigInteger());
    }

    @Test
    public void testShrBy65() {
        BigInteger val = BigInteger.ONE.shiftLeft(65);
        assertEquals(BigInteger.ONE, MemorySize.shr(MemorySize.of(val), 65).asBigInteger());
    }

    @Test
    public void testShrBy127Positive() {
        // 2^127 - 1 >> 127 = 0 (since the MSB of a positive 127-bit value is at position 126)
        assertEquals(MemorySize.ZERO, MemorySize.shr(MemorySize.of(MAX_128), 127));
    }

    @Test
    public void testShrBy127Negative() {
        // -1 >> 127 = -1 (sign extends)
        MemorySize neg = MemorySize.of(-42L);
        assertEquals(MemorySize.of(-1L), MemorySize.shr(neg, 127));
    }

    @Test
    public void testShrCrossWordValue() {
        // value spanning both words, shifted to collapse
        BigInteger val = BigInteger.ONE.shiftLeft(80).add(BigInteger.ONE.shiftLeft(20));
        BigInteger expected = val.shiftRight(40);
        assertEquals(expected, MemorySize.shr(MemorySize.of(val), 40).asBigInteger());
    }

    @Test
    public void testShrNegativeBy64() {
        // -2^64 >> 64 = -1
        BigInteger val = BigInteger.ONE.shiftLeft(64).negate();
        assertEquals(BigInteger.valueOf(-1), MemorySize.shr(MemorySize.of(val), 64).asBigInteger());
    }

    @Test
    public void testShrNegativeBy63() {
        // large negative >> 63
        BigInteger val = BigInteger.ONE.shiftLeft(100).negate();
        BigInteger expected = val.shiftRight(63);
        assertEquals(expected, MemorySize.shr(MemorySize.of(val), 63).asBigInteger());
    }

    @Test
    public void testShrSaturatesAt200() {
        MemorySize pos = MemorySize.of(42L);
        assertEquals(MemorySize.ZERO, MemorySize.shr(pos, 200));
        MemorySize neg = MemorySize.of(-42L);
        assertEquals(MemorySize.of(-1L), MemorySize.shr(neg, 200));
    }

    @Test
    public void testShlSaturatesAt200() {
        MemorySize ms = MemorySize.of(42L);
        assertSame(MemorySize.ZERO, MemorySize.shl(ms, 200));
    }

    // ── Signum tests ──

    @Test
    public void testSignumPositive() {
        assertEquals(1, MemorySize.of("1K").signum());
    }

    @Test
    public void testSignumZero() {
        assertEquals(0, MemorySize.ZERO.signum());
    }

    @Test
    public void testSignumNegative() {
        assertEquals(-1, MemorySize.of(-1L).signum());
    }

    @Test
    public void testSignumPositiveLowOnly() {
        // high == 0 but low != 0 should be positive
        assertEquals(1, MemorySize.of(1L).signum());
    }

    // ── isZero / isNegative / isPositive tests ──

    @Test
    public void testIsZero() {
        assertTrue(MemorySize.ZERO.isZero());
        assertFalse(MemorySize.of(1L).isZero());
        assertFalse(MemorySize.of(-1L).isZero());
        assertFalse(MemorySize.of("1K").isZero());
    }

    @Test
    public void testIsNegative() {
        assertTrue(MemorySize.of(-1L).isNegative());
        assertTrue(MemorySize.of("-1K").isNegative());
        assertFalse(MemorySize.ZERO.isNegative());
        assertFalse(MemorySize.of(1L).isNegative());
    }

    @Test
    public void testIsPositive() {
        assertTrue(MemorySize.of(1L).isPositive());
        assertTrue(MemorySize.of("1K").isPositive());
        assertFalse(MemorySize.ZERO.isPositive());
        assertFalse(MemorySize.of(-1L).isPositive());
    }

    // ── Comparison tests ──

    @Test
    public void testCompareToMemorySize() {
        MemorySize small = MemorySize.of("1K");
        MemorySize large = MemorySize.of("1M");
        assertTrue(small.compareTo(large) < 0);
        assertTrue(large.compareTo(small) > 0);
        assertEquals(0, small.compareTo(MemorySize.of("1K")));
    }

    @Test
    public void testCompareToLong() {
        MemorySize ms = MemorySize.of(1024L);
        assertEquals(0, ms.compareTo(1024L));
        assertTrue(ms.compareTo(1023L) > 0);
        assertTrue(ms.compareTo(1025L) < 0);
    }

    @Test
    public void testCompareToLongWithHighLow() {
        // value > Long.MAX_VALUE
        MemorySize large = MemorySize.of(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE));
        assertTrue(large.compareTo(Long.MAX_VALUE) > 0);
    }

    @Test
    public void testCompareToUnsigned() {
        MemorySize ms = MemorySize.of(1024L);
        assertEquals(0, ms.compareToUnsigned(1024L));
        assertTrue(ms.compareToUnsigned(1023L) > 0);
        assertTrue(ms.compareToUnsigned(1025L) < 0);
    }

    // ── Scale and trailing zeros ──

    @Test
    public void testNumberOfTrailingZeros() {
        assertEquals(10, MemorySize.of("1K").numberOfTrailingZeros());
        assertEquals(20, MemorySize.of("1M").numberOfTrailingZeros());
        assertEquals(0, MemorySize.of(1L).numberOfTrailingZeros());
    }

    @Test
    public void testScale() {
        assertEquals(MemorySize.Scale.B, MemorySize.of(1L).scale());
        assertEquals(MemorySize.Scale.K, MemorySize.of("1K").scale());
        assertEquals(MemorySize.Scale.M, MemorySize.of("3M").scale());
        assertEquals(MemorySize.Scale.B, MemorySize.of(1500L).scale());
    }

    // ── Min/Max ──

    @Test
    public void testMin() {
        MemorySize small = MemorySize.of("1K");
        MemorySize large = MemorySize.of("1M");
        assertSame(small, MemorySize.min(small, large));
        assertSame(small, MemorySize.min(large, small));
    }

    @Test
    public void testMax() {
        MemorySize small = MemorySize.of("1K");
        MemorySize large = MemorySize.of("1M");
        assertSame(large, MemorySize.max(small, large));
        assertSame(large, MemorySize.max(large, small));
    }

    // ── toString ──

    @Test
    public void testToStringZero() {
        assertEquals("0", MemorySize.ZERO.toString());
    }

    @Test
    public void testToStringBytes() {
        assertEquals("100", MemorySize.of(100L).toString());
    }

    @Test
    public void testToStringKilo() {
        assertEquals("1K", MemorySize.of("1K").toString());
    }

    @Test
    public void testToStringMega() {
        assertEquals("3M", MemorySize.of("3M").toString());
    }

    @Test
    public void testToStringNonExactScale() {
        // 1500 bytes not evenly divisible by 1024
        assertEquals("1500", MemorySize.of(1500L).toString());
    }

    @Test
    public void testToStringNegative() {
        assertEquals("-1K", MemorySize.of("1K").neg().toString());
    }

    @Test
    public void testToStringLargeScale() {
        assertEquals("23Y", MemorySize.of("23Y").toString());
    }

    // ── Equals and hashCode ──

    @Test
    public void testEquals() {
        assertEquals(MemorySize.of("1K"), MemorySize.of("1K"));
        assertEquals(MemorySize.of("1024B"), MemorySize.of("1K"));
        assertNotEquals(MemorySize.of("1K"), MemorySize.of("1M"));
    }

    @Test
    public void testHashCodeConsistent() {
        assertEquals(MemorySize.of("1K").hashCode(), MemorySize.of("1024B").hashCode());
    }

    @Test
    public void testEqualsNull() {
        assertNotEquals(null, MemorySize.of("1K"));
    }

    // ── Overflow tests ──

    @Test
    public void testAddOverflow() {
        // two large positive values that exceed 2^127-1
        MemorySize maxHigh = MemorySize.of(BigInteger.ONE.shiftLeft(126));
        assertThrows(ArithmeticException.class, () -> MemorySize.add(maxHigh, maxHigh));
    }

    @Test
    public void testSubOverflow() {
        // most-negative-allowed minus one produces MIN_128 which is forbidden
        MemorySize maxNeg = MemorySize.of(MAX_128.negate());
        assertThrows(ArithmeticException.class, () -> MemorySize.sub(maxNeg, MemorySize.of(1L)));
    }

    @Test
    public void testNegMaxMagnitude() {
        // neg of -(2^127-1) produces 2^127-1
        BigInteger maxNeg = MAX_128.negate();
        assertEquals(MAX_128, MemorySize.of(maxNeg).neg().asBigInteger());
    }

    // ── mul() tests ──

    @Test
    public void testMul() {
        assertEquals(MemorySize.of("300K"), MemorySize.mul(MemorySize.of("100K"), 3));
    }

    @Test
    public void testMulZero() {
        assertSame(MemorySize.ZERO, MemorySize.mul(MemorySize.of("100K"), 0));
    }

    @Test
    public void testMulOne() {
        MemorySize ms = MemorySize.of("100K");
        assertSame(ms, MemorySize.mul(ms, 1));
    }

    @Test
    public void testMulMinusOne() {
        MemorySize ms = MemorySize.of("100K");
        assertEquals(ms.neg(), MemorySize.mul(ms, -1));
    }

    @Test
    public void testMulCarryIntoHigh() {
        // Long.MAX_VALUE * 2 should produce a valid 128-bit result
        MemorySize ms = MemorySize.of(Long.MAX_VALUE);
        BigInteger expected = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(2));
        assertEquals(expected, MemorySize.mul(ms, 2).asBigInteger());
    }

    @Test
    public void testMulNegativeByPositive() {
        MemorySize neg = MemorySize.of("100K").neg();
        assertEquals(MemorySize.of("300K").neg(), MemorySize.mul(neg, 3));
    }

    @Test
    public void testMulNegativeByNegative() {
        MemorySize neg = MemorySize.of("100K").neg();
        assertEquals(MemorySize.of("300K"), MemorySize.mul(neg, -3));
    }

    @Test
    public void testMulOverflow() {
        MemorySize large = MemorySize.of(BigInteger.ONE.shiftLeft(126));
        assertThrows(ArithmeticException.class, () -> MemorySize.mul(large, 4));
    }

    // ── div() tests ──

    @Test
    public void testDiv() {
        assertEquals(MemorySize.of("100K"), MemorySize.div(MemorySize.of("300K"), 3));
    }

    @Test
    public void testDivByZero() {
        assertThrows(ArithmeticException.class, () -> MemorySize.div(MemorySize.of("1K"), 0));
    }

    @Test
    public void testDivByOne() {
        MemorySize ms = MemorySize.of("100K");
        assertSame(ms, MemorySize.div(ms, 1));
    }

    @Test
    public void testDivByMinusOne() {
        MemorySize ms = MemorySize.of("100K");
        assertEquals(ms.neg(), MemorySize.div(ms, -1));
    }

    @Test
    public void testDivTruncation() {
        // 10 / 3 = 3 (truncated toward zero)
        assertEquals(MemorySize.of(3L), MemorySize.div(MemorySize.of(10L), 3));
    }

    @Test
    public void testDivNegativeTruncation() {
        // -10 / 3 = -3 (truncated toward zero)
        assertEquals(MemorySize.of(-3L), MemorySize.div(MemorySize.of(-10L), 3));
    }

    @Test
    public void testDivNegativeDivisor() {
        assertEquals(MemorySize.of("100K").neg(), MemorySize.div(MemorySize.of("300K"), -3));
    }

    @Test
    public void testDivNegativeByNegative() {
        assertEquals(MemorySize.of("100K"), MemorySize.div(MemorySize.of("300K").neg(), -3));
    }

    @Test
    public void testDivLargeDividend() {
        assertEquals(MemorySize.of("1Y"), MemorySize.div(MemorySize.of("23Y"), 23));
    }

    @Test
    public void testDivByLongMinValue() {
        // value = 2^63, divisor = Long.MIN_VALUE (-2^63); result should be -1
        MemorySize val = MemorySize.of(BigInteger.ONE.shiftLeft(63));
        assertEquals(MemorySize.of(-1L), MemorySize.div(val, Long.MIN_VALUE));
    }

    // ── Parsing edge cases for longer strings ──

    private static final BigInteger MAX_128 = BigInteger.ONE.shiftLeft(127).subtract(BigInteger.ONE);
    private static final BigInteger MIN_128 = BigInteger.ONE.shiftLeft(127).negate();

    @Test
    public void testOfLeadingZeros() {
        assertEquals(MemorySize.of("100K"), MemorySize.of("0000100K"));
    }

    @Test
    public void testOfAllZeros() {
        assertSame(MemorySize.ZERO, MemorySize.of("0000000000000000"));
    }

    @Test
    public void testOfMaxValue() {
        // 2^127 - 1 as a raw byte count (39 decimal digits)
        String max = MAX_128.toString();
        assertEquals(MAX_128, MemorySize.of(max).asBigInteger());
    }

    @Test
    public void testOfMinValueOverflows() {
        // -2^127 cannot be parsed (the digit accumulation is always non-negative)
        String min = MIN_128.toString();
        assertThrows(IllegalArgumentException.class, () -> MemorySize.of(min));
    }

    @Test
    public void testOfMaxValuePlusOneOverflows() {
        String onePastMax = MAX_128.add(BigInteger.ONE).toString();
        assertThrows(IllegalArgumentException.class, () -> MemorySize.of(onePastMax));
    }

    @Test
    public void testOfMinValueMinusOneOverflows() {
        String onePastMin = MIN_128.subtract(BigInteger.ONE).toString();
        assertThrows(IllegalArgumentException.class, () -> MemorySize.of(onePastMin));
    }

    @Test
    public void testOfLargeDigitCountOverflowsDuringAccumulation() {
        // 40-digit number: overflows in the multiply-by-10 loop
        assertThrows(IllegalArgumentException.class,
                () -> MemorySize.of("9999999999999999999999999999999999999999"));
    }

    @Test
    public void testOfFitsInDigitsButOverflowsAfterScale() {
        // max scaled value for K is floor((2^127 - 1) / 1024)
        BigInteger maxK = MAX_128.shiftRight(10);
        assertEquals(maxK.shiftLeft(10), MemorySize.of(maxK + "K").asBigInteger());
        // one past that should overflow
        BigInteger tooLargeK = maxK.add(BigInteger.ONE);
        assertThrows(IllegalArgumentException.class, () -> MemorySize.of(tooLargeK + "K"));
    }

    @Test
    public void testOfLargeValueWithHighScale() {
        // a value that fits with E scale but would overflow with Z
        BigInteger maxE = MAX_128.shiftRight(60);
        assertEquals(maxE.shiftLeft(60), MemorySize.of(maxE + "E").asBigInteger());
        BigInteger tooLargeZ = MAX_128.shiftRight(70).add(BigInteger.ONE);
        assertThrows(IllegalArgumentException.class, () -> MemorySize.of(tooLargeZ + "Z"));
    }

    @Test
    public void testOfRoundTripLargeValue() {
        // parse a large value, convert to string, parse again
        MemorySize original = MemorySize.of("23Y");
        MemorySize roundTripped = MemorySize.of(original.toString());
        assertEquals(original, roundTripped);
    }

    @Test
    public void testOfRoundTripNegativeLargeValue() {
        MemorySize original = MemorySize.of("23Y").neg();
        MemorySize roundTripped = MemorySize.of(original.toString());
        assertEquals(original, roundTripped);
    }

    @Test
    public void testOfRoundTripMaxValue() {
        MemorySize max = MemorySize.of(MAX_128);
        MemorySize roundTripped = MemorySize.of(max.toString());
        assertEquals(max, roundTripped);
    }

    @Test
    public void testOfNegativeWithScale() {
        BigInteger expected = KILO.pow(3).multiply(BigInteger.valueOf(-42));
        assertEquals(expected, MemorySize.of("-42G").asBigInteger());
    }

    @Test
    public void testOfMaxValueExactlyAtScaleBoundary() {
        // 2^127 - 1 has trailing zeros at byte scale only, so it can only be expressed as bytes
        // verify it round-trips through toString (which picks the largest exact scale)
        MemorySize max = MemorySize.of(MAX_128);
        String s = max.toString();
        // MAX_128 is odd, so scale is B and toString produces the raw digit string
        assertEquals(MAX_128.toString(), s);
    }

    // ── Parsing values requiring more than 64 significant bits ──

    @Test
    public void testOfValueExceeding64Bits() {
        // 2^64 requires h > 0 during digit accumulation
        BigInteger twoTo64 = BigInteger.ONE.shiftLeft(64);
        assertEquals(twoTo64, MemorySize.of("18446744073709551616").asBigInteger());
    }

    @Test
    public void testOfLargeDecimalSpanningBothWords() {
        // 10^20 is a 21-digit number that exceeds 64 bits
        BigInteger val = BigInteger.TEN.pow(20);
        assertEquals(val, MemorySize.of(val.toString()).asBigInteger());
    }

    @Test
    public void testOfLargeDecimalWithSuffix() {
        // Digit portion exceeds 64 bits, then scaled by K
        BigInteger digits = BigInteger.ONE.shiftLeft(64).add(BigInteger.valueOf(42));
        BigInteger expected = digits.shiftLeft(10);
        assertEquals(expected, MemorySize.of(digits + "K").asBigInteger());
    }

    @Test
    public void testOfNegativeLargeDecimal() {
        BigInteger val = BigInteger.TEN.pow(20).negate();
        assertEquals(val, MemorySize.of("-" + BigInteger.TEN.pow(20)).asBigInteger());
    }

    @Test
    public void testOfLargeNegativeWithSuffix() {
        BigInteger digits = BigInteger.ONE.shiftLeft(64).add(BigInteger.valueOf(999));
        BigInteger expected = digits.shiftLeft(20).negate();
        assertEquals(expected, MemorySize.of("-" + digits + "M").asBigInteger());
    }

    @Test
    public void testOfMaxDigitsPerScale() {
        for (MemorySize.Scale scale : MemorySize.Scale.values) {
            BigInteger maxDigits = MAX_128.shiftRight(scale.shift());
            BigInteger expected = maxDigits.shiftLeft(scale.shift());
            String suffix = scale == MemorySize.Scale.B ? "" : scale.name();
            assertEquals(expected, MemorySize.of(maxDigits + suffix).asBigInteger(),
                    "Max digits for scale " + scale);
        }
    }

    @Test
    public void testOfOneOverMaxDigitsPerScaleOverflows() {
        for (MemorySize.Scale scale : MemorySize.Scale.values) {
            if (scale.shift() == 0) {
                continue;
            }
            BigInteger tooMany = MAX_128.shiftRight(scale.shift()).add(BigInteger.ONE);
            String suffix = scale.name();
            assertThrows(IllegalArgumentException.class,
                    () -> MemorySize.of(tooMany + suffix),
                    "Overflow for scale " + scale);
        }
    }

    @Test
    public void testOfTenTimesMaxLong() {
        // exercises the unsigned multiply high computation with large l
        BigInteger val = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TEN);
        assertEquals(val, MemorySize.of(val.toString()).asBigInteger());
    }

    @Test
    public void testOfMaxLongPlusOne() {
        // 2^63 — the first value whose unsigned low word has the sign bit set
        BigInteger val = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
        assertEquals(val, MemorySize.of(val.toString()).asBigInteger());
    }

    // ── toString(Scale) rounding tests ──
    // Javadoc says "The value is rounded up if it is not exactly representable."
    // Current implementation truncates instead of rounding — these tests expose that bug.

    @Test
    public void testToStringWithScaleExact() {
        assertEquals("1K", MemorySize.of("1K").toString(MemorySize.Scale.K));
        assertEquals("3M", MemorySize.of("3M").toString(MemorySize.Scale.M));
        assertEquals("7G", MemorySize.of("7G").toString(MemorySize.Scale.G));
    }

    @Test
    public void testToStringWithScaleDownToBytes() {
        assertEquals("1024", MemorySize.of("1K").toString(MemorySize.Scale.B));
        assertEquals("3145728", MemorySize.of("3M").toString(MemorySize.Scale.B));
    }

    @Test
    public void testToStringWithScaleRoundsUpPositive() {
        // BUG: 1025 bytes in K: ceil(1025/1024) = 2, but current impl truncates to 1
        assertEquals("2K", MemorySize.of(1025L).toString(MemorySize.Scale.K));
    }

    @Test
    public void testToStringWithScaleSmallPositiveRoundsToOne() {
        // BUG: 1 byte in K: ceil(1/1024) = 1, but current impl truncates to 0 (emitting no digits)
        assertEquals("1K", MemorySize.of(1L).toString(MemorySize.Scale.K));
    }

    @Test
    public void testToStringWithScaleJustUnderBoundary() {
        // BUG: 1023 bytes in K: ceil(1023/1024) = 1, but current impl truncates to 0
        assertEquals("1K", MemorySize.of(1023L).toString(MemorySize.Scale.K));
    }

    @Test
    public void testToStringWithScaleExactBoundary() {
        assertEquals("2K", MemorySize.of(2048L).toString(MemorySize.Scale.K));
    }

    @Test
    public void testToStringWithScaleZero() {
        assertEquals("0", MemorySize.ZERO.toString(MemorySize.Scale.K));
        assertEquals("0", MemorySize.ZERO.toString(MemorySize.Scale.Y));
    }

    @Test
    public void testToStringWithScaleNegativeExact() {
        assertEquals("-1K", MemorySize.of("1K").neg().toString(MemorySize.Scale.K));
    }

    @Test
    public void testToStringWithScaleUpFromNatural() {
        // BUG: 3K in M: ceil(3072/1048576) = 1, but current impl truncates to 0
        assertEquals("1M", MemorySize.of("3K").toString(MemorySize.Scale.M));
    }

    @Test
    public void testToStringWithScaleLargeValue() {
        assertEquals("23Y", MemorySize.of("23Y").toString(MemorySize.Scale.Y));
    }

    @Test
    public void testToStringWithScaleLargeValueDifferentScale() {
        // 1Y = 2^80 bytes. In E scale: 2^80 / 2^60 = 2^20 = 1048576
        assertEquals("1048576E", MemorySize.of("1Y").toString(MemorySize.Scale.E));
    }

    // ── mul() shift-optimization overflow tests ──
    // When the multiplier is a power of 2, mul delegates to shl which doesn't check for overflow.

    @Test
    public void testMulPowerOf2OverflowPositive() {
        // BUG: 2^126 * 2 = 2^127, overflows signed 128-bit
        // shl wraps to -2^127 instead of throwing
        MemorySize val = MemorySize.of(BigInteger.ONE.shiftLeft(126));
        assertThrows(ArithmeticException.class, () -> MemorySize.mul(val, 2));
    }

    @Test
    public void testMulPowerOf2OverflowSubtle() {
        // BUG: (5 * 2^64) * 2^62 = 5 * 2^126 > 2^127-1
        // shl silently drops the high bit of 5, producing 2^126 instead of throwing
        MemorySize val = MemorySize.of(BigInteger.valueOf(5).shiftLeft(64));
        assertThrows(ArithmeticException.class, () -> MemorySize.mul(val, 1L << 62));
    }

    @Test
    public void testMulPowerOf2LargeValid() {
        // 2^62 * 4 = 2^64, valid
        MemorySize val = MemorySize.of(BigInteger.ONE.shiftLeft(62));
        assertEquals(BigInteger.ONE.shiftLeft(64), MemorySize.mul(val, 4).asBigInteger());
    }

    @Test
    public void testMulPowerOf2NegativeProducesMinValue() {
        // -2^126 * 2 = -2^127 = MIN_128, which is now forbidden
        MemorySize val = MemorySize.of(BigInteger.ONE.shiftLeft(126).negate());
        assertThrows(ArithmeticException.class, () -> MemorySize.mul(val, 2));
    }

    @Test
    public void testMulPowerOf2NegativeOverflows() {
        // -2^126 * 4 = -2^128, overflows
        MemorySize val = MemorySize.of(BigInteger.ONE.shiftLeft(126).negate());
        assertThrows(ArithmeticException.class, () -> MemorySize.mul(val, 4));
    }

    @Test
    public void testMulPowerOf2LargeValueValid() {
        // (2^64 + 1) * 2 = 2^65 + 2, fits in 128 bits
        BigInteger a = BigInteger.ONE.shiftLeft(64).add(BigInteger.ONE);
        BigInteger expected = a.multiply(BigInteger.valueOf(2));
        assertEquals(expected, MemorySize.mul(MemorySize.of(a), 2).asBigInteger());
    }

    // ── div() shift-optimization truncation direction tests ──
    // When the divisor is a power of 2, div delegates to shr (arithmetic right shift),
    // which truncates toward negative infinity instead of toward zero.

    @Test
    public void testDivPowerOf2NegativeTruncatesTowardZero() {
        // BUG: -3 / 2 should be -1, but shr gives -2
        assertEquals(MemorySize.of(-1L), MemorySize.div(MemorySize.of(-3L), 2));
    }

    @Test
    public void testDivPowerOf2NegativeSmallQuotient() {
        // BUG: -1 / 2 should be 0, but shr gives -1
        assertEquals(MemorySize.ZERO, MemorySize.div(MemorySize.of(-1L), 2));
    }

    @Test
    public void testDivPowerOf2NegativeExactDivision() {
        // -1024 / 1024 = -1 exactly, no truncation issue
        assertEquals(MemorySize.of(-1L), MemorySize.div(MemorySize.of(-1024L), 1024));
    }

    @Test
    public void testDivPowerOf2NegativeOneOverExact() {
        // BUG: -1025 / 1024 should be -1, but shr gives -2
        assertEquals(MemorySize.of(-1L), MemorySize.div(MemorySize.of(-1025L), 1024));
    }

    @Test
    public void testDivPowerOf2PositiveTruncation() {
        // 3 / 2 = 1 — positive truncation is correct with shr
        assertEquals(MemorySize.of(1L), MemorySize.div(MemorySize.of(3L), 2));
    }

    @Test
    public void testDivPowerOf2LargeNegativeValue() {
        // BUG: large negative value not exactly divisible by 1024
        BigInteger val = BigInteger.ONE.shiftLeft(100).negate().subtract(BigInteger.ONE);
        BigInteger expected = val.divide(BigInteger.valueOf(1024));
        assertEquals(expected, MemorySize.div(MemorySize.of(val), 1024).asBigInteger());
    }

    // ── shl/shr with Integer.MIN_VALUE distance ──
    // -Integer.MIN_VALUE == Integer.MIN_VALUE (overflow), causing infinite recursion.

    @Test
    public void testShlIntegerMinValueDistance() {
        // BUG: causes StackOverflowError from infinite recursion between shl and shr
        // Correct: shl(x, -2^31) = shr(x, 2^31), which saturates since 2^31 > 128
        MemorySize ms = MemorySize.of(42L);
        assertEquals(MemorySize.ZERO, MemorySize.shl(ms, Integer.MIN_VALUE));
    }

    @Test
    public void testShrIntegerMinValueDistance() {
        // BUG: same infinite recursion
        // Correct: shr(x, -2^31) = shl(x, 2^31), which saturates to ZERO
        MemorySize ms = MemorySize.of(42L);
        assertEquals(MemorySize.ZERO, MemorySize.shr(ms, Integer.MIN_VALUE));
    }

    // ── Arithmetic with large (>64-bit) operands ──

    @Test
    public void testAddBothWordsSignificant() {
        BigInteger a = BigInteger.ONE.shiftLeft(100).add(BigInteger.valueOf(Long.MAX_VALUE));
        BigInteger b = BigInteger.ONE.shiftLeft(90).add(BigInteger.valueOf(12345));
        BigInteger expected = a.add(b);
        assertEquals(expected, MemorySize.add(MemorySize.of(a), MemorySize.of(b)).asBigInteger());
    }

    @Test
    public void testSubBothWordsSignificant() {
        BigInteger a = BigInteger.ONE.shiftLeft(100).add(BigInteger.valueOf(Long.MAX_VALUE));
        BigInteger b = BigInteger.ONE.shiftLeft(90).add(BigInteger.valueOf(12345));
        BigInteger expected = a.subtract(b);
        assertEquals(expected, MemorySize.sub(MemorySize.of(a), MemorySize.of(b)).asBigInteger());
    }

    @Test
    public void testMulLargeMultiplicand() {
        BigInteger a = BigInteger.ONE.shiftLeft(70).add(BigInteger.valueOf(999999));
        long b = 123456789L;
        BigInteger expected = a.multiply(BigInteger.valueOf(b));
        assertEquals(expected, MemorySize.mul(MemorySize.of(a), b).asBigInteger());
    }

    @Test
    public void testDivLargeDividendBothWords() {
        BigInteger a = BigInteger.ONE.shiftLeft(100).add(BigInteger.valueOf(Long.MAX_VALUE));
        long b = 7L;
        BigInteger expected = a.divide(BigInteger.valueOf(b));
        assertEquals(expected, MemorySize.div(MemorySize.of(a), b).asBigInteger());
    }

    @Test
    public void testNegLargeValue() {
        BigInteger val = BigInteger.ONE.shiftLeft(100).add(BigInteger.valueOf(42));
        BigInteger expected = val.negate();
        assertEquals(expected, MemorySize.of(val).neg().asBigInteger());
    }

    // ── Comparison with large values ──

    @Test
    public void testCompareToLargeValuesDifferingInLowWord() {
        BigInteger base = BigInteger.ONE.shiftLeft(100);
        MemorySize a = MemorySize.of(base);
        MemorySize b = MemorySize.of(base.add(BigInteger.ONE));
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(MemorySize.of(base)));
    }

    @Test
    public void testCompareToLargeValuesDifferingInHighWord() {
        MemorySize a = MemorySize.of(BigInteger.ONE.shiftLeft(100));
        MemorySize b = MemorySize.of(BigInteger.ONE.shiftLeft(101));
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
    }

    @Test
    public void testCompareToLargeNegativeValues() {
        MemorySize a = MemorySize.of(BigInteger.ONE.shiftLeft(100).negate());
        MemorySize b = MemorySize.of(BigInteger.ONE.shiftLeft(101).negate());
        assertTrue(a.compareTo(b) > 0); // -2^100 > -2^101
        assertTrue(b.compareTo(a) < 0);
    }

    // ── Shift operations with large operands ──

    @Test
    public void testShlLargeValueSmallShift() {
        BigInteger val = BigInteger.ONE.shiftLeft(64).add(BigInteger.valueOf(42));
        BigInteger expected = val.shiftLeft(10);
        assertEquals(expected, MemorySize.shl(MemorySize.of(val), 10).asBigInteger());
    }

    @Test
    public void testShrLargeValueSmallShift() {
        BigInteger val = BigInteger.ONE.shiftLeft(100).add(BigInteger.ONE.shiftLeft(50));
        BigInteger expected = val.shiftRight(30);
        assertEquals(expected, MemorySize.shr(MemorySize.of(val), 30).asBigInteger());
    }

    @Test
    public void testShlNegativeLargeValue() {
        BigInteger val = BigInteger.ONE.shiftLeft(64).negate();
        BigInteger expected = val.shiftLeft(10);
        assertEquals(expected, MemorySize.shl(MemorySize.of(val), 10).asBigInteger());
    }

    @Test
    public void testShrNegativeLargeValue() {
        BigInteger val = BigInteger.ONE.shiftLeft(100).negate();
        BigInteger expected = val.shiftRight(30);
        assertEquals(expected, MemorySize.shr(MemorySize.of(val), 30).asBigInteger());
    }

    // ── numberOfTrailingZeros edge cases ──

    @Test
    public void testNumberOfTrailingZerosZero() {
        assertEquals(128, MemorySize.ZERO.numberOfTrailingZeros());
    }

    @Test
    public void testNumberOfTrailingZerosExactly64() {
        assertEquals(64, MemorySize.of(BigInteger.ONE.shiftLeft(64)).numberOfTrailingZeros());
    }

    @Test
    public void testNumberOfTrailingZerosMaxValue() {
        // MAX_128 = 2^127 - 1 (all bits set), 0 trailing zeros
        assertEquals(0, MemorySize.of(MAX_128).numberOfTrailingZeros());
    }

    // ── scale() edge cases ──

    @Test
    public void testScaleNegativeYottabytes() {
        assertEquals(MemorySize.Scale.Y, MemorySize.of("1Y").neg().scale());
    }

    @Test
    public void testScaleNegativeValue() {
        assertEquals(MemorySize.Scale.K, MemorySize.of("1K").neg().scale());
    }

    // ── toString with large values ──

    @Test
    public void testToStringLargeValueBothWords() {
        BigInteger val = BigInteger.ONE.shiftLeft(100).add(BigInteger.ONE);
        MemorySize ms = MemorySize.of(val);
        assertEquals(val.toString(), ms.toString());
    }

    @Test
    public void testToStringRoundTripLargeValue() {
        BigInteger val = BigInteger.ONE.shiftLeft(100).add(BigInteger.ONE.shiftLeft(10));
        MemorySize ms = MemorySize.of(val);
        assertEquals(ms, MemorySize.of(ms.toString()));
    }

    @Test
    public void testToStringRoundTripLargeNegativeValue() {
        BigInteger val = BigInteger.ONE.shiftLeft(100).add(BigInteger.ONE.shiftLeft(10)).negate();
        MemorySize ms = MemorySize.of(val);
        assertEquals(ms, MemorySize.of(ms.toString()));
    }

    @Test
    public void testToStringLargeNegativeYottabytes() {
        // -(2^80) = -1Y
        assertEquals("-1Y", MemorySize.of("1Y").neg().toString());
    }

    @Test
    public void testToStringLargeNegativeBytesScale() {
        MemorySize val = MemorySize.of("1Y").neg();
        assertEquals("-" + BigInteger.ONE.shiftLeft(80), val.toString(MemorySize.Scale.B));
    }

    @Test
    public void testBigIntegerConstructorRejectsMin128() {
        // MIN_128 (-2^127) should be rejected — the valid range is [-(2^127-1), 2^127-1]
        assertThrows(ArithmeticException.class, () -> MemorySize.of(MIN_128));
    }

    @Test
    public void testOfRawBytesRejectsMin128() {
        // Parsing -2^127 as raw bytes should also fail
        String rawMin = "-" + BigInteger.ONE.shiftLeft(127);
        assertThrows(IllegalArgumentException.class, () -> MemorySize.of(rawMin));
    }

    // ── asLongValue boundary tests ──

    @Test
    public void testAsLongValueMaxLong() {
        assertEquals(Long.MAX_VALUE, MemorySize.of(Long.MAX_VALUE).asLongValue());
    }

    @Test
    public void testAsLongValueMinLong() {
        assertEquals(Long.MIN_VALUE, MemorySize.of(Long.MIN_VALUE).asLongValue());
    }

    @Test
    public void testAsLongValueJustOverMaxLong() {
        MemorySize val = MemorySize.of(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE));
        assertThrows(ArithmeticException.class, val::asLongValue);
    }

    @Test
    public void testAsLongValueJustUnderMinLong() {
        MemorySize val = MemorySize.of(BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE));
        assertThrows(ArithmeticException.class, val::asLongValue);
    }

    // ── BigInteger constructor boundary tests ──

    @Test
    public void testBigIntegerConstructorNear128BitMax() {
        BigInteger val = MAX_128.subtract(BigInteger.ONE);
        assertEquals(val, MemorySize.of(val).asBigInteger());
    }

    @Test
    public void testBigIntegerConstructorOverflows128Bits() {
        BigInteger tooLarge = MAX_128.add(BigInteger.ONE);
        assertThrows(ArithmeticException.class, () -> MemorySize.of(tooLarge));
    }

    @Test
    public void testBigIntegerConstructorUnderflows128Bits() {
        BigInteger tooSmall = MIN_128.subtract(BigInteger.ONE);
        assertThrows(ArithmeticException.class, () -> MemorySize.of(tooSmall));
    }

    // ── alignUp tests ──

    @Test
    public void testAlignUpScaleAlreadyAligned() {
        MemorySize val = MemorySize.of("4K");
        assertSame(val, val.alignUp(MemorySize.Scale.K));
    }

    @Test
    public void testAlignUpScaleRoundsUp() {
        MemorySize val = MemorySize.of(1025); // 1K + 1
        assertEquals(MemorySize.of("2K"), val.alignUp(MemorySize.Scale.K));
    }

    @Test
    public void testAlignUpScaleB() {
        MemorySize val = MemorySize.of(42);
        assertSame(val, val.alignUp(MemorySize.Scale.B));
    }

    @Test
    public void testAlignUpScaleZero() {
        assertSame(MemorySize.ZERO, MemorySize.ZERO.alignUp(MemorySize.Scale.M));
    }

    @Test
    public void testAlignUpScaleNegative() {
        // -1023 rounds up (toward +inf) to 0
        assertEquals(MemorySize.ZERO, MemorySize.of(-1023).alignUp(MemorySize.Scale.K));
    }

    @Test
    public void testAlignUpScaleNegativeAlreadyAligned() {
        MemorySize val = MemorySize.of("-4K");
        assertSame(val, val.alignUp(MemorySize.Scale.K));
    }

    @Test
    public void testAlignUpScaleLargeValue() {
        // 1M + 1 should align up to 2M
        MemorySize val = MemorySize.of(BigInteger.ONE.shiftLeft(20).add(BigInteger.ONE));
        assertEquals(MemorySize.of("2M"), val.alignUp(MemorySize.Scale.M));
    }

    @Test
    public void testAlignUpAmountBasic() {
        MemorySize val = MemorySize.of(5);
        assertEquals(MemorySize.of(8), val.alignUp(8L));
    }

    @Test
    public void testAlignUpAmountAlreadyAligned() {
        MemorySize val = MemorySize.of(16);
        assertSame(val, val.alignUp(16L));
    }

    @Test
    public void testAlignUpAmountOne() {
        MemorySize val = MemorySize.of(42);
        assertSame(val, val.alignUp(1L));
    }

    @Test
    public void testAlignUpAmountRejectsZero() {
        assertThrows(IllegalArgumentException.class, () -> MemorySize.of(1).alignUp(0L));
    }

    @Test
    public void testAlignUpAmountRejectsNegative() {
        assertThrows(IllegalArgumentException.class, () -> MemorySize.of(1).alignUp(-4L));
    }

    @Test
    public void testAlignUpAmountRejectsNonPowerOfTwo() {
        assertThrows(IllegalArgumentException.class, () -> MemorySize.of(1).alignUp(3L));
    }

    @Test
    public void testAlignUpAmountScaleBasic() {
        // 1K + 1 aligned up by 2K = 2K
        MemorySize val = MemorySize.of(1025);
        assertEquals(MemorySize.of("2K"), val.alignUp(2L, MemorySize.Scale.K));
    }

    @Test
    public void testAlignUpAmountScaleAlreadyAligned() {
        MemorySize val = MemorySize.of("4K");
        assertSame(val, val.alignUp(4L, MemorySize.Scale.K));
    }

    @Test
    public void testAlignUpAmountScaleRejectsNonPowerOfTwo() {
        assertThrows(IllegalArgumentException.class, () -> MemorySize.of(1).alignUp(3L, MemorySize.Scale.K));
    }

    @Test
    public void testAlignUpAmountScaleOverflowsWhenShiftExceeds127() {
        // 2^48 * Y = shift 48 + 80 = 128, which exceeds the 127-bit maximum
        assertThrows(ArithmeticException.class, () -> MemorySize.of(1).alignUp(1L << 48, MemorySize.Scale.Y));
    }
}
