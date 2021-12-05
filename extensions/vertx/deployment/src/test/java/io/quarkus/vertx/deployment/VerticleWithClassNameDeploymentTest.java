package io.quarkus.vertx.deployment;

import java.util.concurrent.CountDownLatch;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

/**
 * It should be possible to deploy a verticle from a class name and deployment options.
 */
public class VerticleWithClassNameDeploymentTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanDeployingAVerticleFromClass.class, MyVerticle.class));

    @Test
    public void testDeploymentOfVerticleUsingClassName() {
        String resp1 = RestAssured.get("http://localhost:8080").asString();
        String resp2 = RestAssured.get("http://localhost:8080").asString();
        Assertions.assertTrue(resp1.startsWith("OK"), resp1);
        Assertions.assertTrue(resp2.startsWith("OK"));
        Assertions.assertNotEquals(resp1, resp2);
    }

    public static class BeanDeployingAVerticleFromClass {
        @Inject
        Vertx vertx;

        public void init(@Observes StartupEvent ev) throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(1);
            vertx.deployVerticle(MyVerticle.class.getName(),
                    new DeploymentOptions().setInstances(2),
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
