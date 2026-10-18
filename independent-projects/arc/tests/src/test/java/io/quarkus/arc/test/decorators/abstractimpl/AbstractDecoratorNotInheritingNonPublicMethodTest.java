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
 * Decorated types are interfaces, and interfaces may only have {@code public} and {@code private} methods.
 * Since {@code private} methods are never inherited, all decorated methods that decorators may inherit
 * from superclasses and superinterfaces must be {@code public}, otherwise the code wouldn't compile.
 * This test verifies that the decorator doesn't inherit a {@code private} method from its superclass.
 * Similar test with package-private in the same package and {@code protected} method wouldn't compile.
 * We could add a test with package-private method in a different package, but there's no point.
 */
public class AbstractDecoratorNotInheritingNonPublicMethodTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Converter.class, ConverterImpl.class,
            AbstractConverterDecorator.class, ConverterDecorator.class);

    @Test
    public void testDecoration() {
        Converter converter = Arc.container().instance(Converter.class).get();
        assertEquals(0, converter.convertToInt("HoLa!"));
        assertEquals("hola!", converter.convertToString("HoLa!"));
    }

    interface Converter {
        // not overridden by `ConverterDecorator`, so inherited
        default int convertToInt(String value) {
            return 0;
        }

        // not implemented by abstract `ConverterDecorator`, so forwarded to the contextual instance
        String convertToString(String value);
    }

    @ApplicationScoped
    static class ConverterImpl implements Converter {
        @Override
        public int convertToInt(String value) {
            return value.length();
        }

        @Override
        public String convertToString(String value) {
            return value.toLowerCase();
        }
    }

    static class AbstractConverterDecorator {
        private String convertToString(String value) {
            return "PRIVATE";
        }
    }

    @Decorator
    @Priority(1)
    static abstract class ConverterDecorator extends AbstractConverterDecorator implements Converter {
        @Inject
        @Delegate
        Converter delegate;
    }
}
