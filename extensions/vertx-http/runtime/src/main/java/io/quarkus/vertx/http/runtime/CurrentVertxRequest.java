package io.quarkus.vertx.http.runtime;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;

import io.vertx.ext.web.RoutingContext;

@RequestScoped
public class CurrentVertxRequest {

    private RoutingContext current;
    private Object otherHttpContextObject;

    @Produces
    @RequestScoped
    public RoutingContext getCurrent() {
        return current;
    }

    public CurrentVertxRequest setCurrent(RoutingContext current) {
        this.current = current;
        return this;
    }

    public CurrentVertxRequest setCurrent(RoutingContext current, Object otherHttpContextObject) {
        this.current = current;
        this.otherHttpContextObject = otherHttpContextObject;
        return this;
    }

    public Object getOtherHttpContextObject() {
        return otherHttpContextObject;
    }

    public void setOtherHttpContextObject(Object otherHttpContextObject) {
        this.otherHttpContextObject = otherHttpContextObject;
    }
}
