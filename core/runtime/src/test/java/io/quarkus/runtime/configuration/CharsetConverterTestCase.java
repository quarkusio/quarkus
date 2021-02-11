package io.quarkus.runtime.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CharsetConverterTestCase {

    private CharsetConverter charsetConverter;

    @BeforeEach
    public void setup() {
        charsetConverter = new CharsetConverter();
    }

    @Test
    public void testUTF8Uppercase() {
        assertEquals(charsetConverter.convert("UTF-8"), StandardCharsets.UTF_8);
    }

    @Test
    public void testUTF8Lowercase() {
        assertEquals(charsetConverter.convert("utf-8"), StandardCharsets.UTF_8);
    }

    @Test
    public void testOther() {
        assertEquals(charsetConverter.convert("US-ASCII"), StandardCharsets.US_ASCII);
    }

    @Test
    public void testInvalidCharset() {
        assertThrows(IllegalArgumentException.class, () -> charsetConverter.convert("whatever"));
    }
}
