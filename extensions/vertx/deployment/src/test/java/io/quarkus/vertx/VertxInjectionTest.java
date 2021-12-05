package io.quarkus.vertx;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;

public class VertxInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBeanUsingVertx.class));

    @Test
    public void testVertxInjectionOnStartup() {
        MyBeanUsingVertx bean = Arc.container().instance(MyBeanUsingVertx.class).get();
        Assertions.assertTrue(bean.verify());
    }

    @Test
    public void testEventBusInjectionOnStartup() {
        MyBeanUsingEventBus bean = Arc.container().instance(MyBeanUsingEventBus.class).get();
        Assertions.assertTrue(bean.verify());
    }

    @ApplicationScoped
    public static class MyBeanUsingVertx {

        @Inject
        Vertx vertx;

        @Inject
        io.vertx.mutiny.core.Vertx mutiny;

        boolean ok;

        public boolean verify() {
            return ok;
        }

        public void init(@Observes StartupEvent ev) {
            Assertions.assertNotNull(vertx);
            Assertions.assertNotNull(mutiny);
            ok = true;
        }
    }

    @ApplicationScoped
    public static class MyBeanUsingEventBus {

        @Inject
        EventBus vertx;

        @Inject
        io.vertx.mutiny.core.eventbus.EventBus mutiny;

        boolean ok;

        public boolean verify() {
            return ok;
        }

        public void init(@Observes StartupEvent ev) {
            Assertions.assertNotNull(vertx);
            Assertions.assertNotNull(mutiny);
            ok = true;
        }
    }
}
