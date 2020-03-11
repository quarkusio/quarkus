package io.quarkus.arc.test.injection.privateconstructor;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class PrivateConstructorInjectionTest {

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

        private final Head head;

        @Inject
        private CombineHarvester(Head head) {
            this.head = head;
        }

        public Head getHead() {
            return head;
        }

    }
}
