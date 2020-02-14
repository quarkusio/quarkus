package io.quarkus.vertx.verticles;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import io.quarkus.runtime.StartupEvent;
import io.vertx.mutiny.core.Vertx;

@ApplicationScoped
public class VerticleDeployer {

    void deploy(@Observes StartupEvent event, Vertx vertx, MyBeanVerticle verticle) {
        vertx.deployVerticle(verticle).await().indefinitely();
    }

}
