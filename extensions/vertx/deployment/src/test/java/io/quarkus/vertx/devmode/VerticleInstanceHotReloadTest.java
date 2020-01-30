package io.quarkus.vertx.devmode;

import java.util.concurrent.CountDownLatch;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

public class VerticleInstanceHotReloadTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyVerticle.class, BeanDeployingAVerticleFromInstance.class));

    @Test
    public void testDeploymentOfVerticleInstance() {
        String resp = RestAssured.get("/").asString();
        Assertions.assertTrue(resp.startsWith("ok"));
        test.modifySourceFile(MyVerticle.class, data -> data.replace("ok", "hello"));
        resp = RestAssured.get("/").asString();
        Assertions.assertTrue(resp.startsWith("hello"));
        String resp2 = RestAssured.get("/").asString();
        Assertions.assertEquals(resp, resp2);

    }

    @ApplicationScoped
    public static class BeanDeployingAVerticleFromInstance {
        @Inject
        Vertx vertx;

        public void init(@Observes Router router) throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(1);
            vertx.deployVerticle(new MyVerticle(),
                    ar -> latch.countDown());
            router.get("/").handler(rc -> vertx.eventBus().<String> request("address", "",
                    ar -> rc.response().end(ar.result().body())));
            latch.await();
        }
    }

}
