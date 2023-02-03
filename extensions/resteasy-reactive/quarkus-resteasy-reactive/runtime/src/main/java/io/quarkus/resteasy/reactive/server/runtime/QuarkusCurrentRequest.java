package io.quarkus.resteasy.reactive.server.runtime;

import jakarta.enterprise.context.ContextNotActiveException;

import org.jboss.resteasy.reactive.server.core.CurrentRequest;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.ext.web.RoutingContext;

public class QuarkusCurrentRequest implements CurrentRequest {

    private final CurrentVertxRequest currentVertxRequest;

    public QuarkusCurrentRequest(CurrentVertxRequest currentVertxRequest) {
        this.currentVertxRequest = currentVertxRequest;
    }

    @Override
    public ResteasyReactiveRequestContext get() {
        return (ResteasyReactiveRequestContext) currentVertxRequest.getOtherHttpContextObject();
    }

    @Override
    public void set(ResteasyReactiveRequestContext set) {
        if (set == null) {
            try {
                currentVertxRequest.setOtherHttpContextObject(null);
                currentVertxRequest.setCurrent(null);
            } catch (ContextNotActiveException ignored) {
                // ignored because for HTTP pipelining it can already be closed
            }
        } else {
            currentVertxRequest.setOtherHttpContextObject(set);
            currentVertxRequest.setCurrent(set.unwrap(RoutingContext.class));
        }
    }
}
