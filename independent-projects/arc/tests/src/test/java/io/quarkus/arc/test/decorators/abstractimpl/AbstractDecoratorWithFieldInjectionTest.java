package io.quarkus.arc.test.decorators.abstractimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class AbstractDecoratorWithFieldInjectionTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Converter.class, ToUpperCaseConverter.class,
            TrimConverterDecorator.class, Trimmer.class);

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

        @Inject
        Trimmer trimmer;

        @Override
        public String convert(String value) {
            return delegate.convert(trimmer.trim(value));
        }
    }

    @Dependent
    static class Trimmer {
        public String trim(String value) {
            return value.trim();
        }
    }
}
