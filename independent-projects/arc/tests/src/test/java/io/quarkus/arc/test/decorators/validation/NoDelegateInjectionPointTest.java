package io.quarkus.arc.test.decorators.validation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class NoDelegateInjectionPointTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Converter.class, DecoratorWithNoDelegateInjectionPoint.class).shouldFail().build();

    @Test
    public void testFailure() {
        assertNotNull(container.getFailure());
        assertTrue(
                container.getFailure().getMessage()
                        .contains("DecoratorWithNoDelegateInjectionPoint has no @Delegate injection point"),
                container.getFailure().getMessage());
    }

    interface Converter<T, U> {

        T convert(T value);

    }

    @Priority(1)
    @Decorator
    static class DecoratorWithNoDelegateInjectionPoint implements Converter<String, String> {

        @Override
        public String convert(String value) {
            return null;
        }

    }

}
