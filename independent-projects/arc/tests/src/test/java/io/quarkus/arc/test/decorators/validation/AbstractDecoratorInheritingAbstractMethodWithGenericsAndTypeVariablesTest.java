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

public class AbstractDecoratorInheritingAbstractMethodWithGenericsAndTypeVariablesTest {
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

    static abstract class GoodAbstractDecorator<S, T> {
        abstract T convert2(S value);
    }

    static abstract class BadAbstractDecorator<S, T> {
        abstract T convert3(S value);
    }

    // this is OK, the abstract method comes from a decorated type
    @Decorator
    @Priority(1)
    static abstract class MyGoodDecorator<S, T> extends GoodAbstractDecorator<S, T> implements Converter<S, T> {
        @Inject
        @Delegate
        Converter<S, T> delegate;

        @Override
        public T convert1(S value) {
            return delegate.convert1(value);
        }
    }

    // this is NOT OK, the abstract method does not come from a decorated type
    @Decorator
    @Priority(1)
    static abstract class MyBadDecorator<S, T> extends BadAbstractDecorator<S, T> implements Converter<S, T> {
        @Inject
        @Delegate
        Converter<S, T> delegate;

        @Override
        public T convert1(S value) {
            return delegate.convert1(value);
        }
    }
}
