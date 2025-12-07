package io.quarkus.arc.test.decorators.abstractimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

/**
 * Test that a decorator can declare abstract methods that belong to a decorated type.
 * This is allowed by the CDI specification. Abstract methods in a decorator
 * are simply not decorated - they delegate directly to the underlying bean.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/51196">Issue #51196</a>
 */
public class DecoratorAbstractMethodBelongsToDecoratedTypeTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(
            Service.class,
            ServiceImpl.class,
            ServiceDecorator.class);

    @Test
    public void testDecoratorWithAbstractMethodBelongingToDecoratedType() {
        ServiceImpl service = Arc.container().instance(ServiceImpl.class).get();
        // process() is decorated
        assertEquals("decorated: HELLO", service.process("hello"));
        // getId() is abstract in decorator, so it delegates directly (not decorated)
        assertEquals("test", service.getId());
    }

    interface Service {
        String process(String value);

        String getId();
    }

    @ApplicationScoped
    static class ServiceImpl implements Service {

        @Override
        public String process(String value) {
            return value.toUpperCase();
        }

        @Override
        public String getId() {
            return "test";
        }
    }

    @Priority(1)
    @Decorator
    static abstract class ServiceDecorator implements Service {

        @Inject
        @Delegate
        Service delegate;

        @Override
        public String process(String value) {
            return "decorated: " + delegate.process(value);
        }

        // This abstract method belongs to Service (decorated type)
        // and should be allowed - it will delegate directly without decoration
        @Override
        public abstract String getId();
    }
}
