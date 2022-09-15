package io.quarkus.arc.test.injection.finalfield;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class FinalFieldInjectionTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Head.class, CombineHarvester.class);

    @Test
    public void testInjection() {
        assertNotNull(Arc.container().instance(CombineHarvester.class).get().getHead());
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
        final Head head;

        // The parameter is injected
        CombineHarvester(Head head) {
            this.head = head;
        }

        public Head getHead() {
            return head;
        }

    }
}
