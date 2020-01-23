package io.quarkus.it.vertx.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

public class BareVerticle extends AbstractVerticle {

    @Override
    public void start(Future<Void> done) {
        String address = config().getString("id");
        vertx.eventBus().consumer(address)
                .handler(message -> message.reply("OK-" + address))
                .completionHandler(done);
    }
}
