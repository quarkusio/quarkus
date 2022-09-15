package io.quarkus.arc.test.alternatives.priority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import org.jboss.jandex.AnnotationTarget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests {@link io.quarkus.arc.AlternativePriority} annotation.
 */
public class ComputedAlternativePriorityTest {

    @RegisterExtension
    ArcTestContainer testContainer = ArcTestContainer.builder().beanClasses(MyInterface.class, Foo.class, Producers.class)
            .alternativePriorities((target, stereotypes) -> {
                if (target.kind() == AnnotationTarget.Kind.CLASS) {
                    if (target.asClass().name().toString().equals(Foo.class.getName())) {
                        return 100;
                    }
                } else if (target.kind() == AnnotationTarget.Kind.FIELD || target.kind() == AnnotationTarget.Kind.METHOD) {
                    return 10;
                }
                return null;
            }).build();

    @Test
    public void testComputedPriority() {
        InstanceHandle<MyInterface> myInterface = Arc.container().instance(MyInterface.class);
        assertTrue(myInterface.isAvailable());
        assertEquals(Foo.class.getSimpleName(), myInterface.get().ping());

        InstanceHandle<String> bravo = Arc.container().instance(String.class);
        assertTrue(bravo.isAvailable());
        assertEquals("bravo", bravo.get());

        InstanceHandle<Integer> charlie = Arc.container().instance(Integer.class);
        assertTrue(charlie.isAvailable());
        assertEquals(10, charlie.get());
    }

    static interface MyInterface {
        String ping();
    }

    @Alternative
    @ApplicationScoped
    static class Foo implements MyInterface {

        @Override
        public String ping() {
            return Foo.class.getSimpleName();
        }
    }

    @ApplicationScoped
    static class Producers {

        @Alternative
        @Produces
        static final int CHARLIE = 10;

        @Produces
        @Alternative
        public String bravo() {
            return "bravo";
        }
    }

}
