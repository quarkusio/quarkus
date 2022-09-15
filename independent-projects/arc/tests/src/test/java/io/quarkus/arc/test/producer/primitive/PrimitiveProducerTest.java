package io.quarkus.arc.test.producer.primitive;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class PrimitiveProducerTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(IntProducer.class, LongProducer.class,
            StringArrayProducer.class,
            PrimitiveConsumer.class);

    @Test
    public void testPrimitiveProducers() {
        assertEquals(Long.valueOf(10), Arc.container().instance(Long.class).get());
        assertEquals(Integer.valueOf(10), Arc.container().instance(Integer.class).get());
        PrimitiveConsumer consumer = Arc.container().instance(PrimitiveConsumer.class).get();
        assertEquals(10, consumer.intFoo);
        assertEquals(10l, consumer.longFoo);
        assertEquals(2, consumer.strings.length);
        assertEquals("foo", consumer.strings[0]);
    }

    @Dependent
    static class IntProducer {

        @Produces
        int foo = 10;

    }

    @Dependent
    static class LongProducer {

        @Produces
        long foo() {
            return 10;
        }

    }

    @Dependent
    static class StringArrayProducer {

        @Produces
        String[] strings = { "foo", "bar" };

    }

    @Singleton
    static class PrimitiveConsumer {

        @Inject
        int intFoo;

        @Inject
        long longFoo;

        @Inject
        String[] strings;

    }
}
