package io.quarkus.vertx.http.runtime;

import java.util.function.Function;

import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

public class BasicRoute implements Function<Router, Route> {

    private String path;

    public BasicRoute(String path) {
        this.path = path;
    }

    public BasicRoute() {
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public Route apply(Router router) {
        return router.route(path);
    }
}
