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

public class MultipleSameDecoratedMethodsIndependentTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Foo.class, Bar.class, Alpha.class, Bravo.class,
            AlphaDecorator.class);

    @Test
    public void testDecoration() {
        Alpha alpha = Arc.container().instance(Alpha.class).get();
        Bravo bravo = Arc.container().instance(Bravo.class).get();

        assertEquals(2, alpha.quux()); // decorated
        assertEquals(2, bravo.quux()); // not decorated
    }

    interface Foo {
        int quux();
    }

    interface Bar {
        int quux();
    }

    @ApplicationScoped
    static class Alpha implements Foo, Bar {
        @Override
        public int quux() {
            return 1;
        }
    }

    @ApplicationScoped
    static class Bravo implements Foo, Bar {
        @Override
        public int quux() {
            return 2;
        }
    }

    @Priority(1)
    @Decorator
    static class AlphaDecorator implements Foo, Bar {
        @Inject
        @Delegate
        Alpha delegate;

        @Override
        public int quux() {
            return 1 + delegate.quux();
        }
    }
}
