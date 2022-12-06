package io.quarkus.vertx.http.hotreload;

import jakarta.enterprise.event.Observes;

import io.quarkus.vertx.http.runtime.filters.Filters;

public class DevFilter {

    public void init(@Observes Filters filters) {
        filters.register(rc -> {
            rc.response().putHeader("X-Header", "AAAA");
            rc.next();
        }, 100);
    }
}
