package io.quarkus.vertx.http.runtime;

import java.util.function.Function;

import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

public class BasicRoute implements Function<Router, Route> {

    private String path;

    private Integer order;

    public BasicRoute(String path) {
        this(path, null);
    }

    public BasicRoute(String path, Integer order) {
        this.path = path;
        this.order = order;
    }

    public BasicRoute() {
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    @Override
    public Route apply(Router router) {
        Route route = router.route(path);
        if (order != null) {
            route.order(order);
        }
        return route;
    }
}
