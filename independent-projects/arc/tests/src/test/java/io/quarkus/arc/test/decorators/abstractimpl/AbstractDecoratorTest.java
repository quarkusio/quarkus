package io.quarkus.arc.test.decorators.abstractimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class AbstractDecoratorTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Converter.class, ToUpperCaseConverter.class,
            TrimConverterDecorator.class);

    @Test
    public void testDecoration() {
        ToUpperCaseConverter converter = Arc.container().instance(ToUpperCaseConverter.class).get();
        assertEquals("HELLO", converter.convert(" hello "));
        assertEquals(ToUpperCaseConverter.class.getName(), converter.getId());
    }

    interface Converter<T, U> {

        T convert(T value);

        U getId();

    }

    @ApplicationScoped
    static class ToUpperCaseConverter implements Converter<String, String> {

        @Override
        public String convert(String value) {
            return value.toUpperCase();
        }

        @Override
        public String getId() {
            return ToUpperCaseConverter.class.getName();
        }

    }

    @Priority(1)
    @Decorator
    static abstract class TrimConverterDecorator implements Converter<String, String> {

        @Inject
        @Delegate
        Converter<String, String> delegate;

        @Override
        public String convert(String value) {
            return delegate.convert(value.trim());
        }

    }

}
