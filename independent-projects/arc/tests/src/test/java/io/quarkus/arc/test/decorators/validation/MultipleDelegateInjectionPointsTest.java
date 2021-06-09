package io.quarkus.arc.test.decorators.validation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.test.ArcTestContainer;
import javax.annotation.Priority;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class MultipleDelegateInjectionPointsTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Converter.class, DecoratorWithMultipleDelegateInjetionPoints.class).shouldFail().build();

    @Test
    public void testFailure() {
        assertNotNull(container.getFailure());
        assertTrue(
                container.getFailure().getMessage()
                        .contains("DecoratorWithMultipleDelegateInjetionPoints has multiple @Delegate injection points"),
                container.getFailure().getMessage());
    }

    interface Converter<T> {

        T convert(T value);

    }

    @Priority(1)
    @Decorator
    static class DecoratorWithMultipleDelegateInjetionPoints implements Converter<String> {

        @Inject
        @Delegate
        Converter<String> delegate1;

        @Inject
        @Delegate
        Converter<String> delegate2;

        @Override
        public String convert(String value) {
            return null;
        }

    }

}
