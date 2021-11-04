package io.quarkus.vertx.deployment;

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

public class VertxInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class));

    @Test
    public void test() {
        MyBean bean = Arc.container().instance(MyBean.class).get();
        Assertions.assertTrue(bean.isOk());
    }

    @ApplicationScoped
    public static class MyBean {

        @Inject
        Vertx vertx;

        boolean ok;

        public boolean isOk() {
            return ok;
        }

        public void init(@Observes StartupEvent ev) {
            Assertions.assertNotNull(vertx);
            ok = true;
        }
    }
}
