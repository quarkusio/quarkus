package io.quarkus.arc.test.decorators.disabled;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class DisabledDecoratorInStrictModeTest {
    @RegisterExtension
    ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Converter.class, ToUpperCaseConverter.class, TrimConverterDecorator.class)
            .strictCompatibility(true)
            .build();

    @Test
    public void test() {
        ToUpperCaseConverter converter = Arc.container().instance(ToUpperCaseConverter.class).get();
        assertEquals(" HOLA! ", converter.convert(" holA! "));
    }

    interface Converter<T> {
        T convert(T value);
    }

    @Dependent
    static class ToUpperCaseConverter implements Converter<String> {
        @Override
        public String convert(String value) {
            return value.toUpperCase();
        }
    }

    @Decorator
    // no @Priority, the decorator is disabled in strict mode
    static class TrimConverterDecorator implements Converter<String> {
        @Inject
        @Delegate
        Converter<String> delegate;

        @Override
        public String convert(String value) {
            return delegate.convert(value.trim());
        }
    }
}
