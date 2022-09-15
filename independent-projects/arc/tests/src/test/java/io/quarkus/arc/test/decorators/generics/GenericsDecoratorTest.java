package io.quarkus.arc.test.decorators.generics;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class GenericsDecoratorTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Converter.class, ToUpperCaseConverter.class,
            TrimConverterDecorator.class);

    @Test
    public void testDecoration() {
        ToUpperCaseConverter converter = Arc.container().instance(ToUpperCaseConverter.class).get();
        assertEquals("HELLO", converter.convert(singletonList(singletonList(" hello "))));
        assertEquals(3, converter.ping(1l));
    }

    interface Converter<T, R extends Number> {

        T convert(List<List<T>> value);

        R ping(R value);

    }

    @ApplicationScoped
    static class ToUpperCaseConverter implements Converter<String, Long> {

        @Override
        public String convert(List<List<String>> value) {
            return value.get(0).get(0).toUpperCase();
        }

        @Override
        public Long ping(Long value) {
            return value + 1;
        }

    }

    @Priority(1)
    @Decorator
    static class TrimConverterDecorator implements Converter<String, Long> {

        @Inject
        @Delegate
        Converter<String, Long> delegate;

        @Override
        public String convert(List<List<String>> value) {
            value = singletonList(singletonList(value.get(0).get(0).trim()));
            return delegate.convert(value);
        }

        @Override
        public Long ping(Long value) {
            return delegate.ping(value) + 1;
        }

    }

}
