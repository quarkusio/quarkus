package io.quarkus.arc.test.decorators;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class VoidMethodDecoratorTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Performer.class, MainPerformer.class,
            PerformerDecorator.class);

    @Test
    public void testDecoration() {
        MainPerformer performer = Arc.container().instance(MainPerformer.class).get();

        assertFalse(MainPerformer.DONE.get());
        assertFalse(PerformerDecorator.DONE.get());

        performer.doSomething();

        assertTrue(MainPerformer.DONE.get());
        assertTrue(PerformerDecorator.DONE.get());
    }

    interface Performer {
        void doSomething();
    }

    @ApplicationScoped
    static class MainPerformer implements Performer {
        static final AtomicBoolean DONE = new AtomicBoolean();

        @Override
        public void doSomething() {
            DONE.set(true);
        }
    }

    @Dependent
    @Priority(1)
    @Decorator
    static class PerformerDecorator implements Performer {
        static final AtomicBoolean DONE = new AtomicBoolean();

        @Inject
        @Delegate
        Performer delegate;

        @Override
        public void doSomething() {
            DONE.set(true);
            delegate.doSomething();
        }
    }
}
