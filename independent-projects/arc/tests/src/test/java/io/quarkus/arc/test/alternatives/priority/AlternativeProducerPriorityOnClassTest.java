package io.quarkus.arc.test.alternatives.priority;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.MyQualifier;

public class AlternativeProducerPriorityOnClassTest {
    @RegisterExtension
    ArcTestContainer testContainer = new ArcTestContainer(Producer.class, Consumer.class);

    @Test
    public void testAlternativePriorityResolution() {
        Consumer bean = Arc.container().instance(Consumer.class).get();
        assertNotNull(bean.foo);
        assertNotNull(bean.bar);
    }

    static class Foo {
    }

    @MyQualifier
    static class Bar {
    }

    @Dependent
    @Priority(1000)
    static class Producer {
        @Produces
        @Alternative
        Foo produceFoo = new Foo();

        @Produces
        @Alternative
        @MyQualifier
        Bar produceBar() {
            return new Bar();
        }
    }

    @Dependent
    static class Consumer {
        @Inject
        Foo foo;

        @Inject
        @MyQualifier
        Bar bar;
    }
}
