package io.quarkus.test.vertx;

import java.util.UUID;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;

public class MyMutinyVerticle extends AbstractVerticle {

    private final String id = UUID.randomUUID().toString();

    @Override
    public Uni<Void> asyncStart() {
        return vertx.eventBus().consumer("address")
                .handler(m -> m.reply("ok-" + id))
                .completionHandler();
    }

}
