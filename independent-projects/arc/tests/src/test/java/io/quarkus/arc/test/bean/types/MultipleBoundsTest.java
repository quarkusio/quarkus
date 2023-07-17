package io.quarkus.arc.test.bean.types;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class MultipleBoundsTest {
    @RegisterExtension
    ArcTestContainer container = new ArcTestContainer(BazImpl.class, Consumer.class);

    @Test
    public void multipleBounds() {
        Consumer<?> consumer = Arc.container().instance(new TypeLiteral<Consumer<?>>() {
        }).get();
        assertTrue(consumer.baz instanceof BazImpl<?>);
    }

    interface Foo {
    }

    interface Bar {
    }

    interface Baz<T> {
    }

    static class FooImpl implements Foo {
    }

    @Dependent
    static class BazImpl<T extends Foo & Bar> implements Baz<T> {
    }

    @Dependent
    static class Consumer<T extends FooImpl & Bar> {
        @Inject
        Baz<T> baz;
    }
}
