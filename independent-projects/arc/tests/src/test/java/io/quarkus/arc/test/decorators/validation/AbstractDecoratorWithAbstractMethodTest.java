package io.quarkus.arc.test.decorators.validation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class AbstractDecoratorWithAbstractMethodTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Converter.class, DecoratorWithAbstractMethod.class).shouldFail().build();

    @Test
    public void testFailure() {
        assertNotNull(container.getFailure());
        assertTrue(
                container.getFailure().getMessage().contains("declares abstract methods:"),
                container.getFailure().getMessage());
    }

    interface Converter<T, U> {

        T convert(T value);

    }

    @Priority(1)
    @Decorator
    static abstract class DecoratorWithAbstractMethod implements Converter<String, String> {

        @Inject
        @Delegate
        Converter<String, String> delegate;

        @Override
        public String convert(String value) {
            return delegate.convert(value.trim());
        }

        // this method is not legal
        abstract String anotherConvert();

    }

}
