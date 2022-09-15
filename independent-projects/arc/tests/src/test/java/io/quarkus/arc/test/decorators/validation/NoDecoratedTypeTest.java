package io.quarkus.arc.test.decorators.validation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class NoDecoratedTypeTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Converter.class, DecoratorWithNoDecoratedType.class).shouldFail().build();

    @Test
    public void testFailure() {
        assertNotNull(container.getFailure());
        assertTrue(
                container.getFailure().getMessage().contains("DecoratorWithNoDecoratedType has no decorated type"),
                container.getFailure().getMessage());
    }

    interface Converter<T> {

        T convert(T value);

    }

    @Priority(1)
    @Decorator
    static class DecoratorWithNoDecoratedType {

        @Inject
        @Delegate
        Converter<String> delegate;

        public String convert(String value) {
            return null;
        }

    }

}
