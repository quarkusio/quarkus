package io.quarkus.test.vertx;

import java.util.concurrent.CountDownLatch;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

public class MutinyVerticleClassnameHotReloadTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyMutinyVerticle.class, BeanDeployingAVerticleFromInstance.class));

    @Test
    public void testDeploymentOfMutinyVerticleClass() {
        String resp = RestAssured.get("/").asString();
        Assertions.assertTrue(resp.startsWith("ok"));
        test.modifySourceFile(MyMutinyVerticle.class, data -> data.replace("ok", "hello"));
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
            vertx.deployVerticle(MyMutinyVerticle.class.getName(),
                    ar -> latch.countDown());
            router.get("/").handler(rc -> vertx.eventBus().<String> request("address", "",
                    ar -> rc.response().end(ar.result().body())));
            latch.await();
        }
    }

}
