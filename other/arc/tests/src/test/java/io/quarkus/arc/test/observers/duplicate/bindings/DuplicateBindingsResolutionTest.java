package io.quarkus.arc.test.observers.duplicate.bindings;

import io.quarkus.arc.test.ArcTestContainer;
import java.lang.annotation.Annotation;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.CDI;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests that when you try to resolve observer methods via
 * {@link javax.enterprise.inject.spi.BeanManager#resolveObserverMethods(Object, Annotation...)},
 * you will get an exception if you pass in twice the same annotation that is not repeatable.
 */
public class DuplicateBindingsResolutionTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(BindingTypeA.class, BindingTypeA.BindingTypeABinding.class,
            AnEventType.class, AnObserver.class);

    @Test
    public void testDuplicateBindingTypesWhenResolvingFails() {
        try {
            CDI.current().getBeanManager().resolveObserverMethods(new AnEventType(),
                    new BindingTypeA.BindingTypeABinding("a1"), new BindingTypeA.BindingTypeABinding("a2"));
            Assertions.fail(
                    "BM#resolveObserverMethods should throw IllegalArgumentException if supplied with duplicate bindings");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    public static class AnEventType {
    }

    @Dependent
    public static class AnObserver {
        public boolean wasNotified = false;

        public void observer(@Observes AnEventType event) {
            wasNotified = true;
        }
    }
}
