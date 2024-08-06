package io.quarkus.annotation.processor.documentation.config.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TypeUtilTest {

    @Test
    public void normalizeDurationValueTest() {
        assertEquals("", TypeUtil.normalizeDurationValue(""));
        assertEquals("1S", TypeUtil.normalizeDurationValue("1"));
        assertEquals("1S", TypeUtil.normalizeDurationValue("1S"));
        assertEquals("1S", TypeUtil.normalizeDurationValue("1s"));

        // values are not validated here
        assertEquals("1_000", TypeUtil.normalizeDurationValue("1_000"));
        assertEquals("FOO", TypeUtil.normalizeDurationValue("foo"));
    }
}
