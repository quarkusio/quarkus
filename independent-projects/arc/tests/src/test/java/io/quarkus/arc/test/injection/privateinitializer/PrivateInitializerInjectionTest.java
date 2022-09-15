package io.quarkus.arc.test.injection.privateinitializer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class PrivateInitializerInjectionTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Head.class, CombineHarvester.class);

    @Test
    public void testInjection() {
        assertNotNull(Arc.container().instance(CombineHarvester.class).get().getHead());
    }

    @Dependent
    static class Head {

    }

    @ApplicationScoped
    static class CombineHarvester {

        private Head head;

        @Inject
        private void setHead(Head head) {
            this.head = head;
        }

        public Head getHead() {
            return head;
        }

    }
}
