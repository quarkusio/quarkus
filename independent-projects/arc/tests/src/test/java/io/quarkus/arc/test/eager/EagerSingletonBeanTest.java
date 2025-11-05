package io.quarkus.arc.test.eager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.Eager;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

// this test relies on `ArcTestContainer` _not_ firing `Startup`!
public class EagerSingletonBeanTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBean.class);

    @Test
    public void test() {
        assertFalse(MyBean.constructed);

        Arc.container().beanManager().getEvent().fire(new Startup());

        assertTrue(MyBean.constructed);
    }

    @Eager
    @Singleton
    static class MyBean {
        static boolean constructed = false;

        @PostConstruct
        void init() {
            constructed = true;
        }
    }
}
