package org.acme.reactive.routes;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import io.quarkus.vertx.http.runtime.filters.Filters;

@ApplicationScoped
public class MyFilter {

    public void registerMyFilter(@Observes Filters filters) {
        filters.register(rc -> {
            rc.response().putHeader("X-Header", "intercepting the request");
            rc.next();
        }, 100);
    }
}
