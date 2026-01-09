package io.quarkus.arc.test.decorators.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class AbstractDecoratorDeclaringAbstractMethodWithGenericsTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Converter.class, MyGoodDecorator.class, MyBadDecorator.class)
            .shouldFail()
            .build();

    @Test
    public void trigger() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DefinitionException.class, error);
        assertTrue(error.getMessage().contains("declares abstract method(s) not present on decorated interface(s)"));
        assertTrue(error.getMessage().contains("MyBadDecorator"));
        assertTrue(error.getMessage().contains("convert3"));
        assertFalse(error.getMessage().contains("MyGoodDecorator"));
        assertFalse(error.getMessage().contains("convert1"));
        assertFalse(error.getMessage().contains("convert2"));
    }

    interface Converter<S, T> {
        T convert1(S value);

        T convert2(S value);
    }

    // this is OK, the abstract method comes from a decorated type
    @Decorator
    @Priority(1)
    static abstract class MyGoodDecorator implements Converter<Integer, String> {
        @Inject
        @Delegate
        Converter<Integer, String> delegate;

        @Override
        public String convert1(Integer value) {
            return delegate.convert1(value);
        }

        @Override
        public abstract String convert2(Integer value);
    }

    // this is NOT OK, the abstract method does not come from a decorated type
    @Decorator
    @Priority(1)
    static abstract class MyBadDecorator implements Converter<Integer, String> {
        @Inject
        @Delegate
        Converter<Integer, String> delegate;

        @Override
        public String convert1(Integer value) {
            return delegate.convert1(value);
        }

        public abstract String convert3(Integer value);
    }
}
