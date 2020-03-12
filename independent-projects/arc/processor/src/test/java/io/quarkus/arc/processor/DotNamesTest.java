package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.Basics.index;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    static final class Nested {

        static final class NestedNested {

        }

    }

}
