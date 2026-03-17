package io.quarkus.smallrye.reactivemessaging.hotreload;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;

@ApplicationScoped
public class SomeSink {

    private final List<String> items = new CopyOnWriteArrayList<>();

    @Inject
    Router router;

    @Incoming("my-sink")
    public void sink(String l) {
        items.add(l);
    }

    public void init(@Observes StartupEvent event) {
        router.get("/").handler(rc -> rc.response().end(new JsonArray(items).encode()));
    }

}
