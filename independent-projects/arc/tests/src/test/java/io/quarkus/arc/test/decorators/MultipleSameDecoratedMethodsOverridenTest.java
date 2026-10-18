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

public class MultipleSameDecoratedMethodsOverridenTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Foo.class, Bar.class, Alpha.class, Bravo.class,
            BarDecorator.class);

    @Test
    public void testDecoration() {
        Alpha alpha = Arc.container().instance(Alpha.class).get();
        Bravo bravo = Arc.container().instance(Bravo.class).get();

        assertEquals(2, alpha.quux());
        assertEquals(3, bravo.quux());
    }

    interface Foo {
        int quux();
    }

    interface Bar extends Foo {
        int quux();
    }

    @ApplicationScoped
    static class Alpha implements Bar {
        @Override
        public int quux() {
            return 1;
        }
    }

    @ApplicationScoped
    static class Bravo implements Bar {
        @Override
        public int quux() {
            return 2;
        }
    }

    @Priority(1)
    @Decorator
    static class BarDecorator implements Bar {
        @Inject
        @Delegate
        Bar delegate;

        @Override
        public int quux() {
            return 1 + delegate.quux();
        }
    }
}
