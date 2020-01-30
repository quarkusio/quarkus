package io.quarkus.vertx.devmode;

import java.util.UUID;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

public class MyVerticle extends AbstractVerticle {

    private final String id = UUID.randomUUID().toString();

    @Override
    public void start(Future<Void> done) {
        vertx.eventBus().consumer("address")
                .handler(m -> m.reply("ok-" + id))
                .completionHandler(ar -> done.handle(ar.mapEmpty()));
    }

}
