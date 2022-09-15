package io.quarkus.arc.test.unused;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class RemoveUnusedDecoratorTest extends RemoveUnusedComponentsTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(HasObserver.class, Converter.class, TrimConverterDecorator.class)
            .removeUnusedBeans(true)
            .build();

    @Test
    public void testRemoval() {
        ArcContainer container = Arc.container();
        assertPresent(HasObserver.class);
        assertNotPresent(Converter.class);
        assertTrue(container.beanManager().resolveDecorators(Set.of(Converter.class)).isEmpty());
    }

    @Dependent
    static class HasObserver {

        void observe(@Observes String event) {
        }

    }

    interface Converter<T> {

        T convert(T value);

    }

    @Dependent
    @Priority(1)
    @Decorator
    static class TrimConverterDecorator implements Converter<String> {

        @Inject
        @Delegate
        Converter<String> delegate;

        @Override
        public String convert(String value) {
            return delegate.convert(value.trim());
        }

    }

}
