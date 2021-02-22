package io.quarkus.vertx.http.hotreload;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;

@ApplicationScoped
public class VertxEventBusConsumer {

    @Inject
    Vertx vertx;
    private MessageConsumer<Object> messageConsumer;

    public void init(@Observes StartupEvent event) {
        messageConsumer = vertx.eventBus().consumer("my-address", m -> m.reply("hello"));
    }

    @PreDestroy
    public void stop() {
        messageConsumer.unregister();
    }

}
