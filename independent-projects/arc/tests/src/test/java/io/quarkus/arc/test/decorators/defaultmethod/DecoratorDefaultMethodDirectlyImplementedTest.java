package io.quarkus.arc.test.decorators.defaultmethod;

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

public class DecoratorDefaultMethodDirectlyImplementedTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Converter.class, ConverterImpl.class,
            ConverterDecorator.class);

    @Test
    public void testDecoration() {
        Converter<String> converter = Arc.container().instance(new TypeLiteral<Converter<String>>() {
        }).get();
        assertEquals(6, converter.convertToInt("HoLa!"));
        assertTrue(converter.convertToBoolean("echo"));
        assertFalse(converter.convertToBoolean("ECHO"));
    }

    interface Converter<T> {
        // overridden by both `ConverterImpl` and `ConverterDecorator`
        default int convertToInt(T value) {
            return Integer.MAX_VALUE;
        }

        // overridden by `ConverterDecorator`,
        // not overridden by `ConverterImpl`, so inherited
        default boolean convertToBoolean(T value) {
            return false;
        }
    }

    @ApplicationScoped
    static class ConverterImpl implements Converter<String> {
        @Override
        public int convertToInt(String value) {
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
        public int convertToInt(String value) {
            return 1 + delegate.convertToInt(value);
        }

        @Override
        public boolean convertToBoolean(String value) {
            return value.equals("echo");
        }
    }
}
