package io.quarkus.vertx.http.hotreload;

import jakarta.enterprise.event.Observes;

import io.quarkus.vertx.http.runtime.filters.Filters;

public class NewFilter {

    public void init(@Observes Filters filters) {
        filters.register(rc -> {
            rc.response().putHeader("X-Header-2", "Some new header");
            rc.next();
        }, 100);
    }
}
