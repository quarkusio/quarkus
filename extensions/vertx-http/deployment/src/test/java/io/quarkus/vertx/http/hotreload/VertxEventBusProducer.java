package io.quarkus.vertx.http.hotreload;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

@ApplicationScoped
public class VertxEventBusProducer {

    @Inject
    Router router;

    @Inject
    Vertx vertx;

    public void register(@Observes StartupEvent ev) {
        router.get("/").handler(rc -> vertx.eventBus().<String> request("my-address", "", m -> {
            if (m.failed()) {
                rc.response().end("failed");
            } else {
                rc.response().end(m.result().body());
            }
        }));
    }

}
