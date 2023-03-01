package io.quarkus.arc.test.decorators.validation;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class DecoratorWithObserverTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyDecorator.class, Converter.class)
            .shouldFail()
            .build();

    @Test
    public void trigger() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DefinitionException.class, error);
        assertTrue(error.getMessage().contains("Decorator declares an observer method"));
    }

    interface Converter<T> {
        T convert(String value);
    }

    @Decorator
    @Priority(1)
    static class MyDecorator implements Converter<Number> {
        @Inject
        @Delegate
        Converter<Number> delegate;

        @Override
        public Number convert(String value) {
            return null;
        }

        void observe(@Observes String ignored) {
        }
    }
}
