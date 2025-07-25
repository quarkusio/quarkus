package io.quarkus.vertx.http.hotreload;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.quarkus.runtime.StartupEvent;
import io.vertx.ext.web.Router;

@ApplicationScoped
public class NewBean {

    private final String message = "Hello New World";

    @Inject
    Router router;

    public void register(@Observes StartupEvent ev) {
        router.get("/bean").handler(rc -> rc.response().end(message));
    }

}
