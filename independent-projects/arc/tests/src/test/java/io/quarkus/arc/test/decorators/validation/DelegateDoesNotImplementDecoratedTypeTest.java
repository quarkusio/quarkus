package io.quarkus.arc.test.decorators.validation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DelegateDoesNotImplementDecoratedTypeTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Converter.class, TrimConverterDecorator.class, MyBean.class).shouldFail().build();

    @Test
    public void testFailure() {
        assertNotNull(container.getFailure());
        assertTrue(
                container.getFailure().getMessage().contains("MyBean does not implement the decorated type"),
                container.getFailure().getMessage());
    }

    interface Converter<T> {

        T convert(T value);

    }

    @Dependent
    static class MyBean {

    }

    @Priority(1)
    @Decorator
    static class TrimConverterDecorator implements Converter<String> {

        @Inject
        @Delegate
        MyBean delegate;

        @Override
        public String convert(String value) {
            return null;
        }

    }

}
