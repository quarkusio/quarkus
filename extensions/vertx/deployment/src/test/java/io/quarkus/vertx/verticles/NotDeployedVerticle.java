package io.quarkus.vertx.verticles;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;

@ApplicationScoped
public class NotDeployedVerticle extends AbstractVerticle {

    @Override
    public Uni<Void> asyncStart() {
        return vertx.eventBus().consumer("alpha")
                .handler(m -> m.reply("hello from alpha"))
                .completionHandler();
    }
}
