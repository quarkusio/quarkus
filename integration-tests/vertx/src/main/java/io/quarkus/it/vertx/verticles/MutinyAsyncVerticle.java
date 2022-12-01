package io.quarkus.it.vertx.verticles;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;

public class MutinyAsyncVerticle extends AbstractVerticle {

    @Override
    public Uni<Void> asyncStart() {
        String address = config().getString("id");
        return vertx.eventBus().consumer(address)
                .handler(message -> message.reply("OK-" + address))
                .completionHandler();
    }

}
