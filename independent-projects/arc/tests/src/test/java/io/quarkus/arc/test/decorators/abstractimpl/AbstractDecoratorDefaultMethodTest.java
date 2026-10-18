package io.quarkus.arc.test.decorators.abstractimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

public class AbstractDecoratorDefaultMethodTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Converter.class, ConverterImpl.class,
            ConverterDecorator.class);

    @Test
    public void testDecoration() {
        Converter<String> converter = Arc.container().instance(new TypeLiteral<Converter<String>>() {
        }).get();
        assertEquals(0, converter.convertToInt("HoLa!"));
        assertEquals("hola!", converter.convertToString("HoLa!"));
    }

    interface Converter<T> {
        // not overridden by `ConverterDecorator`, so inherited
        default int convertToInt(T value) {
            return 0;
        }

        // not implemented by abstract `ConverterDecorator`, so forwarded to the contextual instance
        String convertToString(T value);
    }

    @ApplicationScoped
    static class ConverterImpl implements Converter<String> {
        @Override
        public int convertToInt(String value) {
            return value.length();
        }

        @Override
        public String convertToString(String value) {
            return value.toLowerCase();
        }
    }

    @Decorator
    @Priority(1)
    static abstract class ConverterDecorator implements Converter<String> {
        @Inject
        @Delegate
        Converter<String> delegate;
    }
}
