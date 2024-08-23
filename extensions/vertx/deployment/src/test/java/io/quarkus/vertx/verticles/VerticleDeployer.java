package io.quarkus.vertx.verticles;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.quarkus.runtime.StartupEvent;
import io.vertx.mutiny.core.Vertx;

@ApplicationScoped
public class VerticleDeployer {

    @Inject
    Vertx vertx;

    private volatile String deploymentId;

    void deploy(@Observes StartupEvent event, MyBeanVerticle verticle, MyUndeployedVerticle undeployedVerticle) {
        vertx.deployVerticle(verticle).await().indefinitely();
        deploymentId = vertx.deployVerticle(undeployedVerticle).await().indefinitely();
    }

    void undeploy() {
        vertx.undeploy(deploymentId).await().indefinitely();
    }

}
