package io.quarkus.arc.test.injection.constructornoinject;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class SingleNonNoArgConstructorInjectionTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Head.class, CombineHarvester.class);

    @Test
    public void testInjection() {
        assertNotNull(Arc.container().instance(CombineHarvester.class).get().getHead());
    }

    @Dependent
    static class Head {

    }

    @Singleton
    static class CombineHarvester {

        final Head head;

        @Inject
        Head head2;

        public CombineHarvester(Head head) {
            this.head = head;
        }

        public Head getHead() {
            return head;
        }

    }
}
