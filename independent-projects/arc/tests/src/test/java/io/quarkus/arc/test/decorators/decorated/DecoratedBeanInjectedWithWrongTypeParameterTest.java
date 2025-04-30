package io.quarkus.arc.test.decorators.decorated;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Decorated;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class DecoratedBeanInjectedWithWrongTypeParameterTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Converter.class, DecoratedBean.class, TrimConverterDecorator.class)
            .shouldFail()
            .build();

    @Test
    public void testDecoration() {
        assertNotNull(container.getFailure());
        assertTrue(container.getFailure().getMessage().startsWith(
                "Injected @Decorated Bean<> has to use the delegate type as its type parameter. Problematic injection point: "
                        + TrimConverterDecorator.class.getName() + "#decorated"));
    }

    interface Converter<T> {
        T convert(T value);
    }

    @ApplicationScoped
    static class DecoratedBean implements Converter<String> {
        @Override
        public String convert(String value) {
            return "Replaced by the decorator";
        }
    }

    @Dependent
    @Priority(1)
    @Decorator
    static class TrimConverterDecorator implements Converter<String> {
        @Inject
        @Any
        @Delegate
        Converter<String> delegate;

        @Inject
        @Decorated
        Bean<?> decorated;

        @Override
        public String convert(String value) {
            return decorated.getBeanClass().getName() + " " + decorated.getQualifiers().stream()
                    .sorted(Comparator.comparing(a -> a.annotationType().getName())).toList();
        }
    }
}
