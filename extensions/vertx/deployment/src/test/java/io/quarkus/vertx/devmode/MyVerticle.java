package io.quarkus.vertx.devmode;

import java.util.UUID;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.MessageConsumer;

public class MyVerticle extends AbstractVerticle {

    private final String id = UUID.randomUUID().toString();
    private volatile MessageConsumer<Object> messageConsumer;

    @Override
    public void start(Future<Void> done) {
        messageConsumer = vertx.eventBus().consumer("address")
                .handler(m -> m.reply("ok-" + id));
        messageConsumer
                .completionHandler(ar -> done.handle(ar.mapEmpty()));
    }

    @Override
    public void stop() throws Exception {
        messageConsumer.unregister();
    }
}
