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
 * Test that a decorator can inherit abstract methods from a superclass,
 * as long as those methods belong to a decorated type.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/51196">Issue #51196</a>
 */
public class DecoratorAbstractMethodInheritedFromSuperclassTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(
            Service.class,
            ServiceImpl.class,
            AbstractBaseDecorator.class,
            ConcreteDecorator.class);

    @Test
    public void testDecoratorWithAbstractMethodInheritedFromSuperclass() {
        ServiceImpl service = Arc.container().instance(ServiceImpl.class).get();
        // process() is decorated
        assertEquals("decorated: HELLO", service.process("hello"));
        // getId() is abstract (inherited from superclass), delegates directly
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

    // Abstract base class for decorators - not a decorator itself
    static abstract class AbstractBaseDecorator implements Service {
        // This abstract method will be inherited by the actual decorator
        // It belongs to Service, so it should be allowed
        @Override
        public abstract String getId();
    }

    @Priority(1)
    @Decorator
    static abstract class ConcreteDecorator extends AbstractBaseDecorator {

        @Inject
        @Delegate
        Service delegate;

        @Override
        public String process(String value) {
            return "decorated: " + delegate.process(value);
        }

        // getId() is inherited from AbstractBaseDecorator as abstract
        // and should be allowed since it belongs to Service
    }
}
