package io.quarkus.it.vertx.verticles;

import java.util.concurrent.CountDownLatch;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;

@ApplicationScoped
public class VerticleDeployer {

    @Inject
    Vertx vertx;

    public void init(@Observes StartupEvent ev) {
        CountDownLatch latch = new CountDownLatch(5);
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
        latch.countDown();
    }

}
