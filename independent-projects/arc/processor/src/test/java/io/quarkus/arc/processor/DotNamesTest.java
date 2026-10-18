package io.quarkus.arc.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Test;

import io.quarkus.arc.processor.DotNamesTest.Nested.NestedNested;

public class DotNamesTest {

    @Test
    public void testCreate() {
        DotName nested = DotNames.create(Nested.class);
        assertTrue(nested.isComponentized());
        assertEquals("io.quarkus.arc.processor.DotNamesTest$Nested", nested.toString());
        assertEquals("DotNamesTest$Nested", nested.local());
        assertEquals("DotNamesTest$Nested", nested.withoutPackagePrefix());
        assertFalse(nested.isInner());

        DotName nestedNested = DotNames.create(NestedNested.class);
        assertTrue(nestedNested.isComponentized());
        assertEquals("io.quarkus.arc.processor.DotNamesTest$Nested$NestedNested", nestedNested.toString());
        assertEquals("DotNamesTest$Nested$NestedNested", nestedNested.local());
        assertEquals("DotNamesTest$Nested$NestedNested", nestedNested.withoutPackagePrefix());
        assertFalse(nestedNested.isInner());
    }

    static final class Nested {

        final class NestedNested {

        }

    }

}
