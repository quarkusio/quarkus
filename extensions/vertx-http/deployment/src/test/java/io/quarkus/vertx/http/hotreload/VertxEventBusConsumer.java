package io.quarkus.vertx.http.hotreload;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;

@ApplicationScoped
public class VertxEventBusConsumer {

    @Inject
    Vertx vertx;

    public void init(@Observes StartupEvent event) {
        vertx.eventBus().consumer("my-address", m -> m.reply("hello"));
    }

}
