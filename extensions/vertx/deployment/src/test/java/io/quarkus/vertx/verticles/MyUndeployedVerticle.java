package io.quarkus.vertx.verticles;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
                .handler(m -> m.replyAndForget("hello from bravo"))
                .completionHandler();
    }
}
