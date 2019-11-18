package io.quarkus.vertx.http.runtime;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;

import io.vertx.ext.web.RoutingContext;

@RequestScoped
public class CurrentVertxRequest {

    public RoutingContext current;

    @Produces
    @RequestScoped
    public RoutingContext getCurrent() {
        return current;
    }

    public CurrentVertxRequest setCurrent(RoutingContext current) {
        this.current = current;
        return this;
    }

}
