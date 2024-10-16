package io.quarkus.arc.test.decorators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;

public class SimpleDecoratorTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Converter.class, ToUpperCaseConverter.class,
            TrimConverterDecorator.class, UnusedConverterDecorator.class);

    @Test
    public void testDecoration() {
        InstanceHandle<ToUpperCaseConverter> handle = Arc.container().instance(ToUpperCaseConverter.class);
        ToUpperCaseConverter converter = handle.get();
        assertEquals("HOLA!", converter.convert(" holA!"));
        assertEquals(" HOLA!", converter.convertNoDelegation(" holA!"));
        handle.destroy();
        assertTrue(TrimConverterDecorator.CONSTRUCTED.get());
        assertTrue(TrimConverterDecorator.DESTROYED.get());
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

        public String convertNoDelegation(String value) {
            return value.toUpperCase();
        }

    }

    @Dependent
    @Priority(1)
    @Decorator
    static class TrimConverterDecorator implements Converter<String> {

        static final AtomicBoolean CONSTRUCTED = new AtomicBoolean();
        static final AtomicBoolean DESTROYED = new AtomicBoolean();

        @Inject
        @Delegate
        Converter<String> delegate;

        @Override
        public String convert(String value) {
            return delegate.convert(value.trim());
        }

        @PostConstruct
        void init() {
            CONSTRUCTED.set(true);
        }

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }

    }

    @Priority(2)
    @Decorator
    static class UnusedConverterDecorator implements Converter<Integer> {

        @Inject
        @Delegate
        Converter<Integer> delegate;

        @Override
        public Integer convert(Integer value) {
            return 0;
        }

    }

}
