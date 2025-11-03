package io.quarkus.arc.test.unused;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.InterceptionType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;

public class RemoveUnusedInterceptorTest extends RemoveUnusedComponentsTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(HasObserver.class, Alpha.class, AlphaInterceptor.class, Counter.class, Bravo.class,
                    BravoInterceptor.class)
            .removeUnusedBeans(true)
            .build();

    @Test
    public void testRemoval() {
        ArcContainer container = Arc.container();
        assertPresent(HasObserver.class);
        assertNotPresent(Counter.class);
        // Both AlphaInterceptor and BravoInterceptor were removed
        assertTrue(container.beanManager().resolveInterceptors(InterceptionType.AROUND_INVOKE, new Alpha.Literal()).isEmpty());
        assertTrue(container.beanManager().resolveInterceptors(InterceptionType.AROUND_INVOKE, new Bravo.Literal()).isEmpty());

    }

    @Dependent
    static class HasObserver {

        void observe(@Observes String event) {
        }

    }

}
