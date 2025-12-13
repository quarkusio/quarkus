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
 * Test that a decorator can declare abstract methods that belong to a superinterface
 * of a decorated type. The validation should check the entire interface hierarchy.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/51196">Issue #51196</a>
 */
public class DecoratorAbstractMethodFromSuperinterfaceTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(
            BaseService.class,
            ExtendedService.class,
            ExtendedServiceImpl.class,
            ExtendedServiceDecorator.class);

    @Test
    public void testDecoratorWithAbstractMethodFromSuperinterface() {
        ExtendedServiceImpl service = Arc.container().instance(ExtendedServiceImpl.class).get();
        // extendedProcess() is decorated
        assertEquals("decorated extended: HELLO", service.extendedProcess("hello"));
        // baseProcess() is abstract in decorator (from superinterface), delegates directly
        assertEquals("test", service.baseProcess("test"));
    }

    interface BaseService {
        String baseProcess(String value);
    }

    interface ExtendedService extends BaseService {
        String extendedProcess(String value);
    }

    @ApplicationScoped
    static class ExtendedServiceImpl implements ExtendedService {

        @Override
        public String baseProcess(String value) {
            return value;
        }

        @Override
        public String extendedProcess(String value) {
            return value.toUpperCase();
        }
    }

    @Priority(1)
    @Decorator
    static abstract class ExtendedServiceDecorator implements ExtendedService {

        @Inject
        @Delegate
        ExtendedService delegate;

        @Override
        public String extendedProcess(String value) {
            return "decorated extended: " + delegate.extendedProcess(value);
        }

        // This abstract method belongs to BaseService (superinterface of ExtendedService)
        // and should be allowed - it will delegate directly
        @Override
        public abstract String baseProcess(String value);
    }
}
