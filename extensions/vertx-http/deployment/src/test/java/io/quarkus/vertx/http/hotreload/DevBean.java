package io.quarkus.vertx.http.hotreload;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.quarkus.runtime.StartupEvent;
import io.vertx.ext.web.Router;

@ApplicationScoped
public class DevBean {

    private final String message = "Hello World";

    @Inject
    Router router;

    public void register(@Observes StartupEvent ev) {
        router.get("/dev").handler(rc -> rc.response().end(message));
    }

}
