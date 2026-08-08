package io.quarkus.arc.test.eager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.Eager;
import jakarta.enterprise.event.Startup;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

// this test relies on `ArcTestContainer` _not_ firing `Startup`!
public class EagerSingletonProducerFieldTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBean.class, MyProducer.class);

    @Test
    public void test() {
        assertFalse(MyBean.constructed);

        Arc.container().beanManager().getEvent().fire(new Startup());

        assertTrue(MyBean.constructed);
    }

    static class MyBean {
        static boolean constructed = false;

        public MyBean() {
        }

        public MyBean(int unused) {
            constructed = true;
        }
    }

    @Dependent
    static class MyProducer {
        @Eager
        @Singleton
        @Produces
        MyBean produce = new MyBean(0);
    }
}
