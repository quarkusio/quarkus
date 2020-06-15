package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.Basics.index;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.processor.DotNamesTest.Nested.NestedNested;
import java.io.IOException;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DotNamesTest {

    @Test
    public void testSimpleName() throws IOException {
        Index index = index(Nested.class, NestedNested.class, DotNamesTest.class);
        Assertions.assertEquals("Nested",
                DotNames.simpleName(index.getClassByName(DotName.createSimple(Nested.class.getName()))));
        assertEquals("DotNamesTest$Nested",
                DotNames.simpleName(index.getClassByName(DotName.createSimple(Nested.class.getName())).name()));
        assertEquals("NestedNested",
                DotNames.simpleName(index.getClassByName(DotName.createSimple(NestedNested.class.getName()))));
        assertEquals("DotNamesTest$Nested$NestedNested",
                DotNames.simpleName(index.getClassByName(DotName.createSimple(NestedNested.class.getName())).name()));
        assertEquals("DotNamesTest",
                DotNames.simpleName(index.getClassByName(DotName.createSimple(DotNamesTest.class.getName()))));
        assertEquals("DotNamesTest$Nested", DotNames.simpleName("io.quarkus.arc.processor.DotNamesTest$Nested"));
    }

    @Test
    public void testCreate() throws IOException {
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
