package io.quarkus.runtime.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MemorySizeConverterTestCase {
    private MemorySizeConverter memorySizeConverter;

    @BeforeEach
    public void setup() {
        memorySizeConverter = new MemorySizeConverter();
    }

    @Test
    public void testValueNotInCorrectFormat() {
        assertThrows(IllegalArgumentException.class, () -> memorySizeConverter.convert("HJ"));
    }

    @Test
    public void testValueHasNoSuffixShouldBeConvertedToBytes() {
        long expectedMemorySize = 100l;
        MemorySize memorySize = memorySizeConverter.convert("100");
        assertEquals(expectedMemorySize, memorySize.asLongValue());
    }

    @Test
    public void testValueIsInBytes() {
        long expectedMemorySize = 100l;
        MemorySize memorySize = memorySizeConverter.convert("100B");
        assertEquals(expectedMemorySize, memorySize.asLongValue());
        memorySize = memorySizeConverter.convert("100b");
        assertEquals(expectedMemorySize, memorySize.asLongValue());
    }

    @Test
    public void testValueIsInKiloBytes() {
        long expectedMemorySize = MemorySizeConverter.KILO_BYTES.longValue() * 100l;
        MemorySize memorySize = memorySizeConverter.convert("100K");
        assertEquals(expectedMemorySize, memorySize.asLongValue());
        memorySize = memorySizeConverter.convert("100k");
        assertEquals(expectedMemorySize, memorySize.asLongValue());
    }

    @Test
    public void testValueIsInMegaBytes() {
        long expectedMemorySize = MemorySizeConverter.KILO_BYTES.pow(2).longValue() * 100l;
        MemorySize memorySize = memorySizeConverter.convert("100M");
        assertEquals(expectedMemorySize, memorySize.asLongValue());
        memorySize = memorySizeConverter.convert("100m");
        assertEquals(expectedMemorySize, memorySize.asLongValue());
    }

    @Test
    public void testValueIsInGigaBytes() {
        long expectedMemorySize = MemorySizeConverter.KILO_BYTES.pow(3).longValue() * 27l;
        MemorySize memorySize = memorySizeConverter.convert("27G");
        assertEquals(expectedMemorySize, memorySize.asLongValue());
        memorySize = memorySizeConverter.convert("27g");
        assertEquals(expectedMemorySize, memorySize.asLongValue());
    }

    @Test
    public void testValueIsInTeraBytes() {
        long expectedMemorySize = MemorySizeConverter.KILO_BYTES.pow(4).longValue() * 19l;
        MemorySize memorySize = memorySizeConverter.convert("19T");
        assertEquals(expectedMemorySize, memorySize.asLongValue());
        memorySize = memorySizeConverter.convert("19t");
        assertEquals(expectedMemorySize, memorySize.asLongValue());
    }

    @Test
    public void testValueIsInPetaBytes() {
        long expectedMemorySize = MemorySizeConverter.KILO_BYTES.pow(5).longValue() * 31l;
        MemorySize memorySize = memorySizeConverter.convert("31P");
        assertEquals(expectedMemorySize, memorySize.asLongValue());
        memorySize = memorySizeConverter.convert("31p");
        assertEquals(expectedMemorySize, memorySize.asLongValue());
    }

    @Test
    public void testValueIsInExaBytes() {
        BigInteger expectedMemorySize = MemorySizeConverter.KILO_BYTES.pow(6).multiply(BigInteger.valueOf(9));
        MemorySize memorySize = memorySizeConverter.convert("9E");
        assertEquals(expectedMemorySize, memorySize.asBigInteger());
        memorySize = memorySizeConverter.convert("9e");
        assertEquals(expectedMemorySize, memorySize.asBigInteger());
    }

    @Test
    public void testValueIsInZettaBytes() {
        BigInteger expectedMemorySize = MemorySizeConverter.KILO_BYTES.pow(7).multiply(BigInteger.valueOf(5));
        MemorySize memorySize = memorySizeConverter.convert("5Z");
        assertEquals(expectedMemorySize, memorySize.asBigInteger());
        memorySize = memorySizeConverter.convert("5z");
        assertEquals(expectedMemorySize, memorySize.asBigInteger());
    }

    @Test
    public void testValueIsInYottaBytes() {
        BigInteger expectedMemorySize = MemorySizeConverter.KILO_BYTES.pow(8).multiply(BigInteger.valueOf(23));
        MemorySize memorySize = memorySizeConverter.convert("23Y");
        assertEquals(expectedMemorySize, memorySize.asBigInteger());
        memorySize = memorySizeConverter.convert("23y");
        assertEquals(expectedMemorySize, memorySize.asBigInteger());
    }
}
