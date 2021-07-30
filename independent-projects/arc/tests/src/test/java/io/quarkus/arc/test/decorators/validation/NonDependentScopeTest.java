package io.quarkus.arc.test.decorators.validation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.test.ArcTestContainer;
import javax.annotation.Priority;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

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
                                "A decorator must be @Dependent but io.quarkus.arc.test.decorators.validation.NonDependentScopeTest$DecoratorWithWrongScope declares javax.enterprise.context.ApplicationScoped"),
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
