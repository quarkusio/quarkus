package io.quarkus.smallrye.reactivemessaging.hotreload;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;

@ApplicationScoped
public class SomeSink {

    private JsonArray items = new JsonArray();

    @Inject
    Router router;

    @Incoming("my-sink")
    public void sink(String l) {
        items.add(l);
    }

    public void init(@Observes StartupEvent event) {
        router.get("/").handler(rc -> rc.response().end(items.encode()));
    }

}
