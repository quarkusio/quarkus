package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class LiteralSupportTest {

    @Test
    public void testNumbers() {
        assertEquals(Integer.valueOf(43), LiteralSupport.getLiteralValue("43"));
        assertEquals(Long.valueOf(1000), LiteralSupport.getLiteralValue("1000l"));
        assertEquals(Long.valueOf(-10), LiteralSupport.getLiteralValue("-10L"));
        assertEquals(Double.valueOf(1.0d), LiteralSupport.getLiteralValue("1d"));
        assertEquals(Double.valueOf(2.12d), LiteralSupport.getLiteralValue("+2.12d"));
        assertEquals(Double.valueOf(-2.12d), LiteralSupport.getLiteralValue("-2.12d"));
        assertEquals(Double.valueOf(123.4d), LiteralSupport.getLiteralValue("123.4D"));
        assertEquals(Float.valueOf(2.12f), LiteralSupport.getLiteralValue("2.12f"));
        assertEquals(Float.valueOf(2.12f), LiteralSupport.getLiteralValue("2.12F"));
    }

    @Test
    public void testBooleans() {
        assertEquals(Boolean.TRUE, LiteralSupport.getLiteralValue("true"));
        assertEquals(Boolean.FALSE, LiteralSupport.getLiteralValue("false"));
    }

    @Test
    public void testNull() {
        assertNull(LiteralSupport.getLiteralValue("null"));
    }

    @Test
    public void testStrings() {
        assertEquals("foo", LiteralSupport.getLiteralValue("'foo'"));
        assertEquals("foo", LiteralSupport.getLiteralValue("\"foo\""));
    }

}
