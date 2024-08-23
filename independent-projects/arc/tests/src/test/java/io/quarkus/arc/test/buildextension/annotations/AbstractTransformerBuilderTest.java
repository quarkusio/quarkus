package io.quarkus.arc.test.buildextension.annotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractList;

import jakarta.enterprise.context.Dependent;

import org.junit.jupiter.api.Test;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;

public abstract class AbstractTransformerBuilderTest {

    @Test
    public void testVetoed() {
        ArcContainer arc = Arc.container();
        assertTrue(arc.instance(Seven.class).isAvailable());
        // One is vetoed
        assertFalse(arc.instance(One.class).isAvailable());
        assertEquals(Integer.valueOf(7), Integer.valueOf(arc.instance(Seven.class).get().size()));

        // Scope annotation and @Inject are added by transformer
        InstanceHandle<IWantToBeABean> iwant = arc.instance(IWantToBeABean.class);
        assertTrue(iwant.isAvailable());
        assertEquals(Integer.valueOf(7), Integer.valueOf(iwant.get().size()));

        assertEquals(Integer.valueOf(7), arc.select(int.class).get());
    }

    // => add @Dependent here
    static class IWantToBeABean {

        // => add @Inject here
        Seven seven;

        // => add @Produces here
        public int size() {
            return seven.size();
        }

        // => do not add @Produces here
        @Simple
        public int anotherSize() {
            return seven.size();
        }

    }

    @Dependent
    static class Seven extends AbstractList<Integer> {

        @Override
        public Integer get(int index) {
            return Integer.valueOf(7);
        }

        @Simple
        @Override
        public int size() {
            return 7;
        }

    }

    // => add @Vetoed here
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
