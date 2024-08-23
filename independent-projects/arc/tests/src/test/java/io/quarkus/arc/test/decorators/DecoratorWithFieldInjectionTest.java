package io.quarkus.arc.test.decorators;

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

public class DecoratorWithFieldInjectionTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Converter.class, ToUpperCaseConverter.class,
            TrimConverterDecorator.class, Trimmer.class);

    @Test
    public void testDecoration() {
        ToUpperCaseConverter converter = Arc.container().instance(ToUpperCaseConverter.class).get();
        assertEquals("HOLA!", converter.convert(" holA! "));
    }

    interface Converter<T> {
        T convert(T value);
    }

    @ApplicationScoped
    static class ToUpperCaseConverter implements Converter<String> {
        @Override
        public String convert(String value) {
            return value.toUpperCase();
        }
    }

    @Dependent
    @Priority(1)
    @Decorator
    static class TrimConverterDecorator implements Converter<String> {
        @Inject
        @Delegate
        Converter<String> delegate;

        @Inject
        Trimmer trimmer;

        @Override
        public String convert(String value) {
            return delegate.convert(trimmer.trim(value));
        }
    }

    @Dependent
    static class Trimmer {
        public String trim(String str) {
            return str.trim();
        }
    }
}
