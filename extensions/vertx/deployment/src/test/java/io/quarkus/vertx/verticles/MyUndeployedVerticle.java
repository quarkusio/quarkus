package io.quarkus.vertx.verticles;

import javax.enterprise.context.ApplicationScoped;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;

@ApplicationScoped
public class MyUndeployedVerticle extends AbstractVerticle {

    @Override
    public Uni<Void> asyncStart() {
        return vertx.eventBus().consumer("bravo")
                .handler(m -> m.replyAndForget("hello from bravo"))
                .completionHandler();
    }
}
