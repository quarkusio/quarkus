package io.quarkus.arc.test.injection.staticfield;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import java.util.concurrent.atomic.AtomicInteger;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class StaticFieldInjectionTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Head.class, CombineHarvester.class);

    @Test
    public void testInjection() {
        assertNotNull(Arc.container().instance(CombineHarvester.class).get());
        assertNotNull(CombineHarvester.head);
        assertEquals(1, Head.COUNTER.get());
    }

    @Dependent
    static class Head {

        static final AtomicInteger COUNTER = new AtomicInteger(0);

        public Head() {
            COUNTER.incrementAndGet();
        }

    }

    @Dependent
    static class CombineHarvester {

        // This one is ignored
        @Inject
        static Head head;

        // The parameter is injected
        CombineHarvester(Head head) {
            CombineHarvester.head = head;
        }

    }
}
