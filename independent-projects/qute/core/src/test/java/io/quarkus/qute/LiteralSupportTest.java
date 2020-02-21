package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class LiteralSupportTest {

    @Test
    public void testNumbers() {
        assertEquals(Integer.valueOf(43), LiteralSupport.getLiteral("43"));
        assertEquals(Long.valueOf(1000), LiteralSupport.getLiteral("1000l"));
        assertEquals(Long.valueOf(-10), LiteralSupport.getLiteral("-10L"));
        assertEquals(Double.valueOf(1.0d), LiteralSupport.getLiteral("1d"));
        assertEquals(Double.valueOf(2.12d), LiteralSupport.getLiteral("+2.12d"));
        assertEquals(Double.valueOf(-2.12d), LiteralSupport.getLiteral("-2.12d"));
        assertEquals(Double.valueOf(123.4d), LiteralSupport.getLiteral("123.4D"));
        assertEquals(Float.valueOf(2.12f), LiteralSupport.getLiteral("2.12f"));
        assertEquals(Float.valueOf(2.12f), LiteralSupport.getLiteral("2.12F"));
    }

    @Test
    public void testBooleans() {
        assertEquals(Boolean.TRUE, LiteralSupport.getLiteral("true"));
        assertEquals(Boolean.FALSE, LiteralSupport.getLiteral("false"));
    }

    @Test
    public void testNull() {
        assertNull(LiteralSupport.getLiteral("null"));
    }

    @Test
    public void testStrings() {
        assertEquals("foo", LiteralSupport.getLiteral("'foo'"));
        assertEquals("foo", LiteralSupport.getLiteral("\"foo\""));
    }

}
