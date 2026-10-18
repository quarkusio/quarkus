package io.quarkus.vertx.http.runtime;

import java.util.function.Consumer;
import java.util.function.Function;

import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

public class BasicRoute implements Function<Router, Route> {

    private String name;

    private String path;

    private Integer order;

    private Consumer<Route> customizer;

    public BasicRoute(String path) {
        this(null, path);
    }

    public BasicRoute(String path, Integer order) {
        this(null, path, order);
    }

    public BasicRoute(String path, Integer order, Consumer<Route> customizer) {
        this(null, path, order, customizer);
    }

    public BasicRoute(String name, String path) {
        this(name, path, null);
    }

    public BasicRoute(String name, String path, Integer order) {
        this.name = name;
        this.path = path;
        this.order = order;
    }

    public BasicRoute(String name, String path, Integer order, Consumer<Route> customizer) {
        this.name = name;
        this.path = path;
        this.order = order;
        this.customizer = customizer;
    }

    public BasicRoute() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Consumer<Route> getCustomizer() {
        return customizer;
    }

    public BasicRoute setCustomizer(Consumer<Route> customizer) {
        this.customizer = customizer;
        return this;
    }

    @Override
    public Route apply(Router router) {
        Route route = router.route(path);
        if (name != null) {
            route.setName(name);
        }
        if (order != null) {
            route.order(order);
        }
        if (customizer != null) {
            customizer.accept(route);
        }
        return route;
    }
}
