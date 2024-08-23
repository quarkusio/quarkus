package io.quarkus.it.vertx.verticles;

import java.util.concurrent.CountDownLatch;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;

@ApplicationScoped
public class VerticleDeployer {

    @Inject
    Vertx vertx;

    public void init(@Observes StartupEvent ev) {
        CountDownLatch latch = new CountDownLatch(6);
        vertx.deployVerticle(BareVerticle::new, new DeploymentOptions().setConfig(new JsonObject()
                .put("id", "bare")))
                .subscribe().with(x -> latch.countDown());

        vertx.deployVerticle(BareVerticle.class.getName(), new DeploymentOptions().setConfig(new JsonObject()
                .put("id", "bare-classname")))
                .subscribe().with(x -> latch.countDown());

        vertx.deployVerticle(MutinyAsyncVerticle::new, new DeploymentOptions().setConfig(new JsonObject()
                .put("id", "mutiny")))
                .subscribe().with(x -> latch.countDown());

        vertx.deployVerticle(MutinyAsyncVerticle.class.getName(), new DeploymentOptions().setConfig(new JsonObject()
                .put("id", "mutiny-classname")))
                .subscribe().with(x -> latch.countDown());

        vertx.deployVerticle(MdcVerticle.class.getName(), new DeploymentOptions().setConfig(new JsonObject()
                .put("id", "mdc")))
                .subscribe().with(x -> latch.countDown());

        latch.countDown();
    }

}
