package io.quarkus.it.vertx.verticles;

import java.util.concurrent.CountDownLatch;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.StartupEvent;
import io.vertx.axle.core.Vertx;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class VerticleDeployer {

    @Inject
    Vertx vertx;

    public void init(@Observes StartupEvent ev) {
        CountDownLatch latch = new CountDownLatch(5);
        vertx.deployVerticle(BareVerticle::new, new DeploymentOptions().setConfig(new JsonObject()
                .put("id", "bare")))
                .thenAccept(x -> latch.countDown());

        vertx.deployVerticle(BareVerticle.class.getName(), new DeploymentOptions().setConfig(new JsonObject()
                .put("id", "bare-classname")))
                .thenAccept(x -> latch.countDown());

        vertx.deployVerticle(RxVerticle::new, new DeploymentOptions().setConfig(new JsonObject()
                .put("id", "rx")))
                .thenAccept(x -> latch.countDown());

        vertx.deployVerticle(RxVerticle.class.getName(), new DeploymentOptions().setConfig(new JsonObject()
                .put("id", "rx-classname")))
                .thenAccept(x -> latch.countDown());
        latch.countDown();
    }

}
