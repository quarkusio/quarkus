package io.quarkus.arc.test.injection.assignability.generics;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Vetoed;
import javax.inject.Inject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class RawTypeAssignabilityTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(MyProducer.class, MyConsumer.class, Foo.class);

    @Test
    public void testAssignabilityWithRawType() {
        ArcContainer container = Arc.container();
        MyConsumer consumer = container.instance(MyConsumer.class).get();
        Assert.assertEquals(String.class.toString(), consumer.pingRaw());
        Assert.assertEquals(String.class.toString(), consumer.pingObject());
        Assert.assertEquals(Long.class.toString(), consumer.pingLong());
        Assert.assertEquals(Long.class.toString(), consumer.pingWild());
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
