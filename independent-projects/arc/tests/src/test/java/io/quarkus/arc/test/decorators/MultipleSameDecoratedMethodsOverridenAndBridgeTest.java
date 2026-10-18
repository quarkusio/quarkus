package io.quarkus.arc.test.decorators;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class MultipleSameDecoratedMethodsOverridenAndBridgeTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Alpha.class, Bravo.class, Sorted.class, SortedDecorator.class);

    @Test
    public void testDecoration() {
        Alpha alpha = Arc.container().instance(Alpha.class).get();
        Bravo bravo = Arc.container().instance(Bravo.class).get();

        // The decorator reverses priorities
        assertEquals(1, alpha.compareTo(bravo));
    }

    interface Sorted extends Comparable<Sorted> {
        int priority();

        @Override
        default int compareTo(Sorted other) {
            return Integer.compare(priority(), other.priority());
        }
    }

    @ApplicationScoped
    static class Alpha implements Sorted {
        @Override
        public int priority() {
            return 1;
        }
    }

    @ApplicationScoped
    static class Bravo implements Sorted {
        @Override
        public int priority() {
            return 2;
        }
    }

    @Priority(1)
    @Decorator
    static class SortedDecorator implements Sorted {
        @Inject
        @Delegate
        Sorted delegate;

        @Override
        public int compareTo(Sorted other) {
            return Integer.compare(other.priority(), delegate.priority());
        }

        @Override
        public int priority() {
            return delegate.priority();
        }
    }
}
