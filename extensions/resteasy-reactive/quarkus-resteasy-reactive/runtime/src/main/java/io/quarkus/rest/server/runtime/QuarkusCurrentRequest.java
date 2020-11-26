package io.quarkus.rest.server.runtime;

import org.jboss.resteasy.reactive.server.core.CurrentRequest;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

import io.quarkus.vertx.http.runtime.CurrentVertxRequest;

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
        currentVertxRequest.setOtherHttpContextObject(set);
    }
}
