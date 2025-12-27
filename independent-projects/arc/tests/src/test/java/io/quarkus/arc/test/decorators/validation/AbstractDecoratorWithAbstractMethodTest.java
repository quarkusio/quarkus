package io.quarkus.arc.test.decorators.validation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class AbstractDecoratorWithAbstractMethodTest {

    interface Converter<T> {
        T convert(T value);
    }

    abstract static class AbstractDecoratorBase<T> {
        abstract T process(T item);
    }

    @Priority(1)
    @Decorator
    static abstract class InvalidDecorator implements Converter<String> {

        @Inject
        @Delegate
        Converter<String> delegate;

        @Override
        public abstract String convert(String value);

        abstract String anotherConvert();

    }

    @Priority(1)
    @Decorator
    static abstract class InheritingDecorator extends AbstractDecoratorBase<String> implements Converter<String> {

        @Inject
        @Delegate
        Converter<String> delegate;

        @Override
        public String convert(String value) {
            return delegate.convert(process(value));
        }

    }

    @Priority(1)
    @Decorator
    static abstract class GenericHierarchyDecorator<T> extends AbstractDecoratorBase<T> implements Converter<T> {

        @Inject
        @Delegate
        Converter<T> delegate;

        @Override
        public T convert(T value) {
            return delegate.convert(process(value));
        }

    }

    @RegisterExtension
    public ArcTestContainer invalidContainer = ArcTestContainer.builder()
            .beanClasses(Converter.class, InvalidDecorator.class).shouldFail().build();

    @RegisterExtension
    public ArcTestContainer inheritingContainer = ArcTestContainer.builder()
            .beanClasses(Converter.class, InheritingDecorator.class)
            .build();

    @RegisterExtension
    public ArcTestContainer genericHierarchyContainer = ArcTestContainer.builder()
            .beanClasses(Converter.class, GenericHierarchyDecorator.class)
            .build();

    @Test
    public void testDecoratorWithExtraAbstractMethodFails() {
        assertNotNull(invalidContainer.getFailure());
        Throwable rootCause = invalidContainer.getFailure();
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        assertTrue(rootCause instanceof DefinitionException);
        assertTrue(rootCause.getMessage().contains("abstract decorator")
                || rootCause.getMessage().contains("abstract method")
                || rootCause.getMessage().contains("declares abstract methods"));
    }

    @Test
    public void testInheritedAbstractMethodIsAllowed() {
        assertNull(inheritingContainer.getFailure());
    }

    @Test
    public void testGenericDecoratorWithClassHierarchy() {
        assertNull(genericHierarchyContainer.getFailure());
    }

}