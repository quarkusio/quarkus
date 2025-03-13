package io.quarkus.arc.test.decorators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class DecoratorDefaultMethodTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Converter.class, ToLengthConverter.class,
            ConverterDecorator.class);

    @SuppressWarnings("serial")
    @Test
    public void testDecoration() {
        Converter<String> converter = Arc.container().instance(new TypeLiteral<Converter<String>>() {
        }).get();
        assertEquals(5, converter.convert("Hola!"));
        assertTrue(converter.convertToBoolean("echo"));
        assertFalse(converter.convertToBoolean("echos"));
    }

    interface Converter<T> {

        default int convert(T value) {
            return Integer.MAX_VALUE;
        }

        // this method is intentionally not overriden by ToLengthConverter
        default boolean convertToBoolean(T value) {
            return false;
        }
    }

    @ApplicationScoped
    static class ToLengthConverter implements Converter<String> {
        @Override
        public int convert(String value) {
            return value.length();
        }
    }

    @Priority(1)
    @Decorator
    static class ConverterDecorator implements Converter<String> {

        @Inject
        @Delegate
        Converter<String> delegate;

        @Override
        public int convert(String value) {
            return delegate.convert(value);
        }

        @Override
        public boolean convertToBoolean(String value) {
            return value.equals("echo");
        }

    }

}
