package io.quarkus.vertx.deployment;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.Vertx;

public class VertxCommonProducerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new File("src/test/resources/lorem.txt"), "files/lorem.txt")
                    .addClasses(BeanUsingBareVertx.class));

    @Inject
    BeanUsingBareVertx beanUsingVertx;

    @Test
    public void testVertxInjection() throws Exception {
        beanUsingVertx.verify();
    }

    @ApplicationScoped
    static class BeanUsingBareVertx {

        @Inject
        Vertx vertx;

        public void verify() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            vertx.fileSystem().readFile("files/lorem.txt", ar -> {
                if (ar.failed()) {
                    ar.cause().printStackTrace();
                } else {
                    latch.countDown();
                }
            });
            latch.await(5, TimeUnit.SECONDS);
        }

    }
}
