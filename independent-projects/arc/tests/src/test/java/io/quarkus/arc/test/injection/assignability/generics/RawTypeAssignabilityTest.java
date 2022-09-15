package io.quarkus.arc.test.injection.assignability.generics;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Vetoed;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class RawTypeAssignabilityTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyProducer.class, MyConsumer.class, Foo.class);

    @Test
    public void testAssignabilityWithRawType() {
        ArcContainer container = Arc.container();
        MyConsumer consumer = container.instance(MyConsumer.class).get();
        Assertions.assertEquals(String.class.toString(), consumer.pingRaw());
        Assertions.assertEquals(String.class.toString(), consumer.pingObject());
        Assertions.assertEquals(Long.class.toString(), consumer.pingLong());
        Assertions.assertEquals(Long.class.toString(), consumer.pingWild());
    }

    @ApplicationScoped
    static class MyConsumer {

        @Inject
        Foo rawFoo;

        @Inject
        Foo<Object> objectFoo;

        @Inject
        Foo<?> wildFoo;

        @Inject
        Foo<Long> longFoo;

        public String pingWild() {
            return wildFoo.ping();
        }

        public String pingRaw() {
            return rawFoo.ping();
        }

        public String pingObject() {
            return objectFoo.ping();
        }

        public String pingLong() {
            return longFoo.ping();
        }
    }

    @ApplicationScoped
    static class MyProducer {

        @Produces
        public Foo produceRaw() {
            return new Foo("foo");
        }

        @Produces
        public Foo<Long> produceLong() {
            return new Foo<>(1l);
        }
    }

    @Vetoed
    static class Foo<T> {

        T type;

        public Foo(T type) {
            this.type = type;
        }

        public String ping() {
            return type.getClass().toString();
        }
    }
}
