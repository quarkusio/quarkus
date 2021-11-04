package io.quarkus.test.vertx;

import java.util.UUID;
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

public class ConsumeUuidEventHotReloadTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(UuidMessageConsumer.class, BeanDeployingAVerticleFromInstance.class));

    private static final String SAMPLE_UUID = "38400000-8cf0-11bd-b23e-10b96e4ef00d";

    @Test
    public void testUuidMessageConsumption() {
        String resp = RestAssured.get("/").asString();
        Assertions.assertEquals("test-" + SAMPLE_UUID, resp);
        test.modifySourceFile(UuidMessageConsumer.class, data -> data.replace("test-", "other-"));
        resp = RestAssured.get("/").asString();
        Assertions.assertEquals("other-" + SAMPLE_UUID, resp);
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
            router.get("/").handler(rc -> vertx.eventBus().<String> request("event", UUID.fromString(SAMPLE_UUID),
                    ar -> rc.response().end(ar.result().body())));
            latch.await();
        }
    }

}
