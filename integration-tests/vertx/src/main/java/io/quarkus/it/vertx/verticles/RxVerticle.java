package io.quarkus.it.vertx.verticles;

import io.reactivex.Completable;
import io.vertx.reactivex.core.AbstractVerticle;

public class RxVerticle extends AbstractVerticle {

    @Override
    public Completable rxStart() {
        String address = config().getString("id");
        return vertx.eventBus().consumer(address)
                .handler(message -> message.reply("OK-" + address))
                .rxCompletionHandler();
    }

}
