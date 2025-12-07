package io.quarkus.arc.test.decorators.abstractimpl;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

/**
 * Test that a decorator cannot declare abstract methods that do not belong
 * to any decorated type. This should result in a DefinitionException.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/51196">Issue #51196</a>
 */
public class DecoratorAbstractMethodNotBelongingToDecoratedTypeTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Service.class, ServiceImpl.class, InvalidDecorator.class)
            .shouldFail()
            .build();

    @Test
    public void testDecoratorWithAbstractMethodNotBelongingToDecoratedType() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DefinitionException.class, error);
        assertTrue(error.getMessage().contains("abstract methods that do not belong to a decorated type"));
        assertTrue(error.getMessage().contains("unrelatedMethod"));
    }

    interface Service {
        String process(String value);
    }

    @ApplicationScoped
    static class ServiceImpl implements Service {

        @Override
        public String process(String value) {
            return value.toUpperCase();
        }
    }

    @Priority(1)
    @Decorator
    static abstract class InvalidDecorator implements Service {

        @Inject
        @Delegate
        Service delegate;

        @Override
        public String process(String value) {
            return "decorated: " + delegate.process(value);
        }

        // This abstract method does NOT belong to Service (decorated type)
        // and should cause a DefinitionException
        public abstract void unrelatedMethod();
    }
}
