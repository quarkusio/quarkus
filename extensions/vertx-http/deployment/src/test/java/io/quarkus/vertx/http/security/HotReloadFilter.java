package io.quarkus.vertx.http.security;

import jakarta.enterprise.event.Observes;

import io.quarkus.vertx.http.runtime.filters.Filters;

public class HotReloadFilter {

    public void init(@Observes Filters filters) {
        filters.register(rc -> {
            rc.response().putHeader("X-Header", "AAAA");
            rc.next();
        }, 100);
    }

}
