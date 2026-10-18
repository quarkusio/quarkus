package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class IdentifiersTest {

    @Test
    public void testIsValid() {
        assertTrue(Identifiers.isValid("foo"));
        assertTrue(Identifiers.isValid("foo-bar"));
        assertTrue(Identifiers.isValid("fíí_"));
        assertFalse(Identifiers.isValid("foo bar"));
        assertFalse(Identifiers.isValid(""));
        assertFalse(Identifiers.isValid("   "));
        assertTrue(Identifiers.isValid("%ů="));
    }

}
