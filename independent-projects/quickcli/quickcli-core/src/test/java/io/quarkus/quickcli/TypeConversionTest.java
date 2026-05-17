package io.quarkus.quickcli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class TypeConversionTest {

    // --- Primitives ---

    @Test
    void convertInt() {
        assertEquals(42, TypeConverter.convert("42", int.class));
        assertEquals(42, TypeConverter.convert("42", Integer.class));
        assertEquals(-1, TypeConverter.convert("-1", int.class));
    }

    @Test
    void convertLong() {
        assertEquals(123456789L, TypeConverter.convert("123456789", long.class));
        assertEquals(123456789L, TypeConverter.convert("123456789", Long.class));
    }

    @Test
    void convertShort() {
        assertEquals((short) 10, TypeConverter.convert("10", short.class));
        assertEquals((short) 10, TypeConverter.convert("10", Short.class));
    }

    @Test
    void convertByte() {
        assertEquals((byte) 127, TypeConverter.convert("127", byte.class));
        assertEquals((byte) 127, TypeConverter.convert("127", Byte.class));
    }

    @Test
    void convertFloat() {
        assertEquals(3.14f, TypeConverter.convert("3.14", float.class));
        assertEquals(3.14f, TypeConverter.convert("3.14", Float.class));
    }

    @Test
    void convertDouble() {
        assertEquals(2.718, TypeConverter.convert("2.718", double.class));
        assertEquals(2.718, TypeConverter.convert("2.718", Double.class));
    }

    @Test
    void convertChar() {
        assertEquals('x', TypeConverter.convert("x", char.class));
        assertEquals('x', TypeConverter.convert("x", Character.class));
    }

    @Test
    void convertString() {
        assertEquals("hello", TypeConverter.convert("hello", String.class));
    }

    @Test
    void convertNull() {
        assertNull(TypeConverter.convert(null, String.class));
    }

    // --- Boolean ---

    @Test
    void convertBooleanTrue() {
        assertTrue(TypeConverter.convert("true", boolean.class));
        assertTrue(TypeConverter.convert("TRUE", Boolean.class));
        assertTrue(TypeConverter.convert("yes", boolean.class));
        assertTrue(TypeConverter.convert("YES", Boolean.class));
        assertTrue(TypeConverter.convert("1", boolean.class));
    }

    @Test
    void convertBooleanFalse() {
        assertFalse(TypeConverter.convert("false", boolean.class));
        assertFalse(TypeConverter.convert("FALSE", Boolean.class));
        assertFalse(TypeConverter.convert("no", boolean.class));
        assertFalse(TypeConverter.convert("NO", Boolean.class));
        assertFalse(TypeConverter.convert("0", boolean.class));
    }

    @Test
    void convertBooleanInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
                TypeConverter.convert("maybe", boolean.class));
    }

    // --- Common types ---

    @Test
    void convertFile() {
        File f = TypeConverter.convert("/tmp/test.txt", File.class);
        assertEquals(new File("/tmp/test.txt").getPath(), f.getPath());
    }

    @Test
    void convertPath() {
        Path p = TypeConverter.convert("/tmp/test.txt", Path.class);
        assertEquals(Path.of("/tmp/test.txt"), p);
    }

    @Test
    void convertBigDecimal() {
        assertEquals(new BigDecimal("123.456"), TypeConverter.convert("123.456", BigDecimal.class));
    }

    @Test
    void convertBigInteger() {
        assertEquals(new BigInteger("99999999999"), TypeConverter.convert("99999999999", BigInteger.class));
    }

    @Test
    void convertURI() {
        assertEquals(URI.create("https://example.com"), TypeConverter.convert("https://example.com", URI.class));
    }

    // --- Enums ---

    enum Color { RED, GREEN, BLUE }

    @Test
    void convertEnum() {
        assertEquals(Color.RED, TypeConverter.convert("RED", Color.class));
        assertEquals(Color.BLUE, TypeConverter.convert("BLUE", Color.class));
    }

    @Test
    void convertEnumInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
                TypeConverter.convert("YELLOW", Color.class));
    }

    // --- Custom converters ---

    @Test
    void customConverter() {
        TypeConverter.register(StringBuilder.class, StringBuilder::new);
        StringBuilder sb = TypeConverter.convert("test", StringBuilder.class);
        assertEquals("test", sb.toString());
    }

    // --- Type checks ---

    @Test
    void isBooleanType() {
        assertTrue(TypeConverter.isBooleanType(boolean.class));
        assertTrue(TypeConverter.isBooleanType(Boolean.class));
        assertFalse(TypeConverter.isBooleanType(int.class));
        assertFalse(TypeConverter.isBooleanType(String.class));
    }

    @Test
    void hasConverter() {
        assertTrue(TypeConverter.hasConverter(String.class));
        assertTrue(TypeConverter.hasConverter(int.class));
        assertTrue(TypeConverter.hasConverter(Color.class));
        assertFalse(TypeConverter.hasConverter(Object.class));
    }

    // --- Error cases ---

    @Test
    void noConverterThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                TypeConverter.convert("test", Object.class));
    }

    @Test
    void invalidIntThrows() {
        assertThrows(NumberFormatException.class, () ->
                TypeConverter.convert("abc", int.class));
    }
}
