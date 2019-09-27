package io.quarkus.vertx.deployment;

import java.io.File;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.axle.core.buffer.Buffer;
import io.vertx.core.Vertx;

public class VertxProducerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new File("src/test/resources/lorem.txt"), "files/lorem.txt")
                    .addClasses(BeanUsingBareVertx.class)
                    .addClasses(BeanUsingAxleVertx.class)
                    .addClasses(BeanUsingRXVertx.class));

    @Inject
    BeanUsingBareVertx beanUsingVertx;

    @Inject
    BeanUsingAxleVertx beanUsingAxle;

    @Inject
    BeanUsingRXVertx beanUsingRx;

    @Test
    public void testVertxInjection() throws Exception {
        beanUsingVertx.verify();
        beanUsingAxle.verify();
        beanUsingRx.verify();
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

    @ApplicationScoped
    static class BeanUsingAxleVertx {

        @Inject
        io.vertx.axle.core.Vertx vertx;

        public void verify() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            CompletionStage<Buffer> stage = vertx.fileSystem().readFile("files/lorem.txt");
            stage.thenAccept(buffer -> latch.countDown());
            latch.await(5, TimeUnit.SECONDS);
        }

    }

    @ApplicationScoped
    static class BeanUsingRXVertx {

        @Inject
        io.vertx.reactivex.core.Vertx vertx;

        public void verify() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            vertx.fileSystem().rxReadFile("src/test/resources/lorem.txt")
                    .subscribe(buffer -> latch.countDown());
            latch.await(5, TimeUnit.SECONDS);
        }

    }
}
