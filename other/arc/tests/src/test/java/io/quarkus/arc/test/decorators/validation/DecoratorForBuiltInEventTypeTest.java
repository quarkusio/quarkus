package io.quarkus.arc.test.decorators.validation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Priority;
import io.quarkus.arc.test.ArcTestContainer;
import java.lang.annotation.Annotation;
import java.util.concurrent.CompletionStage;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.event.Event;
import javax.enterprise.event.NotificationOptions;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DecoratorForBuiltInEventTypeTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(BadDecorator.class)
            .shouldFail().build();

    @Test
    public void testFailure() {
        assertNotNull(container.getFailure());
        assertTrue(
                container.getFailure().getMessage().contains("Decorating built-in bean types is not supported!"),
                container.getFailure().getMessage());
    }

    @Decorator
    @Priority(1)
    static class BadDecorator<T> implements Event<T> {

        @Delegate
        @Inject
        Event<T> delegate;

        @Override
        public void fire(T event) {

        }

        @Override
        public Event<T> select(Annotation... qualifiers) {
            return null;
        }

        @Override
        public <U extends T> Event<U> select(Class<U> subtype, Annotation... qualifiers) {
            return null;
        }

        @Override
        public <U extends T> Event<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
            return null;
        }

        @Override
        public <U extends T> CompletionStage<U> fireAsync(U event, NotificationOptions options) {
            return null;
        }

        @Override
        public <U extends T> CompletionStage<U> fireAsync(U event) {
            return null;
        }
    }
}
