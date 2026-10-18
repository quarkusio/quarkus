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

public class SimpleDecoratorOverloadingTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Converter.class, SimpleConverter.class,
            ConverterDecorator.class);

    @Test
    public void testDecoration() {
        SimpleConverter converter = Arc.container().instance(SimpleConverter.class).get();
        assertEquals("HOLA!", converter.convert(" holA!"));
        assertEquals(42, converter.convert(42));
    }

    interface Converter {

        int convert(int value);

        String convert(String value);

    }

    @ApplicationScoped
    static class SimpleConverter implements Converter {

        @Override
        public String convert(String value) {
            return value.toUpperCase();
        }

        @Override
        public int convert(int value) {
            return -1 * value;
        }

    }

    @Dependent
    @Priority(1)
    @Decorator
    static class ConverterDecorator implements Converter {

        @Inject
        @Delegate
        Converter delegate;

        @Override
        public String convert(String value) {
            return delegate.convert(value.trim());
        }

        @Override
        public int convert(int value) {
            return -1 * delegate.convert(value);
        }

    }

}
