package io.quarkus.arc.test.decorators.validation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class NonDependentScopeTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Converter.class, DecoratorWithWrongScope.class).shouldFail().build();

    @Test
    public void testFailure() {
        assertNotNull(container.getFailure());
        assertTrue(
                container.getFailure().getMessage()
                        .contains(
                                "A decorator must be @Dependent but io.quarkus.arc.test.decorators.validation.NonDependentScopeTest$DecoratorWithWrongScope declares jakarta.enterprise.context.ApplicationScoped"),
                container.getFailure().getMessage());
    }

    interface Converter<T, U> {

        T convert(T value);

    }

    @ApplicationScoped
    @Priority(1)
    @Decorator
    static class DecoratorWithWrongScope implements Converter<String, String> {

        @Inject
        @Delegate
        Converter<String, String> delegate;

        @Override
        public String convert(String value) {
            return null;
        }

    }

}
