package io.quarkus.arc.test.vetoed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import java.util.AbstractList;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Vetoed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class VetoedTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Seven.class, One.class);

    @Test
    public void testVetoed() {
        ArcContainer arc = Arc.container();
        assertTrue(arc.instance(Seven.class).isAvailable());
        // One is vetoed
        assertFalse(arc.instance(One.class).isAvailable());
        assertEquals(Integer.valueOf(7), Integer.valueOf(arc.instance(Seven.class).get().size()));
    }

    @Dependent
    static class Seven extends AbstractList<Integer> {

        @Override
        public Integer get(int index) {
            return Integer.valueOf(7);
        }

        @Override
        public int size() {
            return 7;
        }

    }

    @Vetoed
    @Dependent
    static class One extends AbstractList<Integer> {

        @Override
        public Integer get(int index) {
            return Integer.valueOf(1);
        }

        @Override
        public int size() {
            return 1;
        }

    }

}
