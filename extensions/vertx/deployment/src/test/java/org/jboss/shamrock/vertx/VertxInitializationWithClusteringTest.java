package org.jboss.shamrock.vertx;

import io.vertx.core.Vertx;
import org.jboss.shamrock.test.ShamrockUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.util.Objects;

public class VertxInitializationWithClusteringTest {

    @RegisterExtension
    static final ShamrockUnitTest config = new ShamrockUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SimpleBean.class)
                    .addAsResource(new File("src/test/resources/vertx-config-with-cluster.properties"), "META-INF/microprofile-config.properties"));

    @Inject
    SimpleBean simpleBean;

    @Test

    public void testThatTheInjectionFailedAsWeDoNotHaveAClusterManager() {
        Assertions.assertThrows(IllegalStateException.class, () -> simpleBean.verify());
    }


    @ApplicationScoped
    static class SimpleBean {

        @Inject
        Vertx vertx;

        void verify() {
            Objects.requireNonNull(vertx);
        }
    }

}
