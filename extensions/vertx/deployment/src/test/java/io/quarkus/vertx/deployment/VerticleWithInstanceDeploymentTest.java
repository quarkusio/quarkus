package io.quarkus.vertx.deployment;

import java.util.concurrent.CountDownLatch;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

/**
 * It should be possible to deploy a verticle from a class name and deployment options.
 */
public class VerticleWithInstanceDeploymentTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BeanDeployingAVerticleFromInstance.class, MyVerticle.class));

    @Test
    public void testDeploymentOfVerticleInstance() {
        String resp1 = RestAssured.get("http://localhost:8080").asString();
        String resp2 = RestAssured.get("http://localhost:8080").asString();
        Assertions.assertTrue(resp1.startsWith("OK"));
        Assertions.assertTrue(resp2.startsWith("OK"));
        Assertions.assertNotEquals(resp1, resp2);
    }

    public static class BeanDeployingAVerticleFromInstance {
        @Inject
        Vertx vertx;

        public void init(@Observes StartupEvent ev) throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(2);
            vertx.deployVerticle(new MyVerticle(),
                    ar -> latch.countDown());
            vertx.deployVerticle(new MyVerticle(),
                    ar -> latch.countDown());
            latch.await();
        }
    }

    public static class MyVerticle extends AbstractVerticle {

        @Override
        public void start(Promise<Void> done) {
            vertx.createHttpServer()
                    .requestHandler(req -> req.response().end("OK-" + Thread.currentThread().getName()))
                    .listen(8080, ar -> done.handle(ar.mapEmpty()));
        }

    }

}
