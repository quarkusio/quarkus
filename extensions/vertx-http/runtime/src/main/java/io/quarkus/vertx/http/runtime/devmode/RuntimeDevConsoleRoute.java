package io.quarkus.vertx.http.runtime.devmode;

import java.util.function.Consumer;

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Route;

public class RuntimeDevConsoleRoute implements Consumer<Route> {

    private String method;

    public RuntimeDevConsoleRoute() {
    }

    public RuntimeDevConsoleRoute(String method) {
        this.method = method;
    }

    public String getMethod() {
        return method;
    }

    public RuntimeDevConsoleRoute setMethod(String method) {
        this.method = method;
        return this;
    }

    @Override
    public void accept(Route route) {
        route.method(HttpMethod.valueOf(method))
                .order(-100);
    }
}
