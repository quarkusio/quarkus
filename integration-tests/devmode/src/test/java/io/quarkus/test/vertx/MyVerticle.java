package io.quarkus.test.vertx;

import java.util.UUID;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;

public class MyVerticle extends AbstractVerticle {

    private final String id = UUID.randomUUID().toString();
    private volatile MessageConsumer<Object> messageConsumer;

    @Override
    public void start(Promise<Void> done) {
        messageConsumer = vertx.eventBus().consumer("address")
                .handler(m -> m.reply("ok-" + id));
        messageConsumer
                .completionHandler(ar -> done.handle(ar.mapEmpty()));
    }

    @Override
    public void stop() {
        messageConsumer.unregister();
    }
}
