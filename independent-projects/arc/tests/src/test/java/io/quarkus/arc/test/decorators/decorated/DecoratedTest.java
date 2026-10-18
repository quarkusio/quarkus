package io.quarkus.arc.test.decorators.decorated;

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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.MyQualifier;

public class DecoratedTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Converter.class, DecoratedBean.class,
            TrimConverterDecorator.class, MyQualifier.class);

    @Test
    public void testDecoration() {
        DecoratedBean bean = Arc.container().instance(DecoratedBean.class, new MyQualifier.Literal()).get();
        Assertions.assertEquals(
                DecoratedBean.class.getName() + " [@io.quarkus.arc.test.MyQualifier(), @jakarta.enterprise.inject.Any()]",
                bean.convert("any value"));
    }

    interface Converter<T> {

        T convert(T value);

    }

    @ApplicationScoped
    @MyQualifier
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
        Bean<Converter<String>> decorated;

        @Override
        public String convert(String value) {
            return decorated.getBeanClass().getName() + " " + decorated.getQualifiers().stream()
                    .sorted(Comparator.comparing(a -> a.annotationType().getName())).toList();
        }

    }
}
