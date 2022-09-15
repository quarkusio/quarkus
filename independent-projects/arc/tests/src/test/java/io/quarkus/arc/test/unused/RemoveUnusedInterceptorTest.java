package io.quarkus.arc.test.unused;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.enterprise.util.AnnotationLiteral;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class RemoveUnusedInterceptorTest extends RemoveUnusedComponentsTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(HasObserver.class, Alpha.class, AlphaInterceptor.class, Counter.class, Bravo.class,
                    BravoInterceptor.class)
            .removeUnusedBeans(true)
            .build();

    @SuppressWarnings("serial")
    @Test
    public void testRemoval() {
        ArcContainer container = Arc.container();
        assertPresent(HasObserver.class);
        assertNotPresent(Counter.class);
        // Both AlphaInterceptor and BravoInterceptor were removed
        assertTrue(container.beanManager().resolveInterceptors(InterceptionType.AROUND_INVOKE, new AnnotationLiteral<Alpha>() {
        }).isEmpty());
        assertTrue(container.beanManager().resolveInterceptors(InterceptionType.AROUND_INVOKE, new AnnotationLiteral<Bravo>() {
        }).isEmpty());

    }

    @Dependent
    static class HasObserver {

        void observe(@Observes String event) {
        }

    }

}
