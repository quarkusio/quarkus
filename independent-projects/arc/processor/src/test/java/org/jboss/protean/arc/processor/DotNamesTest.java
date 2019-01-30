package org.jboss.protean.arc.processor;

import static org.jboss.protean.arc.processor.Basics.index;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.protean.arc.processor.DotNamesTest.Nested.NestedNested;
import org.junit.Test;

public class DotNamesTest {

    @Test
    public void testSimpleName() throws IOException {
        Index index = index(Nested.class, NestedNested.class, DotNamesTest.class);
        assertEquals("Nested", DotNames.simpleName(index.getClassByName(DotName.createSimple(Nested.class.getName()))));
        assertEquals("DotNamesTest$Nested", DotNames.simpleName(index.getClassByName(DotName.createSimple(Nested.class.getName())).name()));
        assertEquals("NestedNested", DotNames.simpleName(index.getClassByName(DotName.createSimple(NestedNested.class.getName()))));
        assertEquals("DotNamesTest$Nested$NestedNested", DotNames.simpleName(index.getClassByName(DotName.createSimple(NestedNested.class.getName())).name()));
        assertEquals("DotNamesTest", DotNames.simpleName(index.getClassByName(DotName.createSimple(DotNamesTest.class.getName()))));
        assertEquals("DotNamesTest$Nested", DotNames.simpleName("org.jboss.protean.arc.processor.DotNamesTest$Nested"));
    }

    static final class Nested {
        
        static final class NestedNested {
            
        }

    }

}
