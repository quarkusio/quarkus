package org.jboss.shamrock.vertx;

import io.vertx.core.Vertx;
import org.jboss.shamrock.test.ShamrockUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Objects;

public class VertxProducerTest {

    @RegisterExtension
    static final ShamrockUnitTest config = new ShamrockUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SimpleBean.class));

    @Inject
    SimpleBean simpleBean;

    @Test
    public void testVertxInjection() {
        simpleBean.verify();
    }


    @ApplicationScoped
    static class SimpleBean {

        @Inject
        Vertx vertx;

        void verify() {
            Objects.requireNonNull(vertx);
            if (! System.getProperty("vertx.disableDnsResolver").equalsIgnoreCase("true")) {
                throw new AssertionError("Async DNS should have been disabled");
            }
        }
    }

}
