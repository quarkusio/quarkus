package io.quarkus.vertx.verticles;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;

@ApplicationScoped
public class MyUndeployedVerticle extends AbstractVerticle {

    // Injecting @Singleton bean
    @Inject
    EventBus eventBus;

    @Override
    public Uni<Void> asyncStart() {
        return vertx.eventBus().consumer("bravo")
                .handler(m -> m.reply("hello from bravo"))
                .completionHandler();
    }
}
