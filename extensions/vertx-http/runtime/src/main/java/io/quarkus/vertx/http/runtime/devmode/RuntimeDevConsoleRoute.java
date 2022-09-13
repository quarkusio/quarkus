package io.quarkus.vertx.http.runtime.devmode;

import java.util.function.Consumer;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

public class RuntimeDevConsoleRoute implements Consumer<Route> {

    private String method;

    private Handler<RoutingContext> bodyHandler;

    public RuntimeDevConsoleRoute() {
    }

    public RuntimeDevConsoleRoute(String method, Handler<RoutingContext> hasBodyHandler) {
        this.method = method;
        this.bodyHandler = hasBodyHandler;
    }

    public String getMethod() {
        return method;
    }

    public Handler<RoutingContext> getBodyHandler() {
        return bodyHandler;
    }

    public void setBodyHandler(Handler<RoutingContext> bodyHandler) {
        this.bodyHandler = bodyHandler;
    }

    public RuntimeDevConsoleRoute setMethod(String method) {
        this.method = method;
        return this;
    }

    @Override
    public void accept(Route route) {
        route.method(HttpMethod.valueOf(method))
                .order(-100);
        if (bodyHandler != null) {
            route.handler(bodyHandler);
        }
    }
}
