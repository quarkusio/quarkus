package io.quarkus.vertx.web.runtime;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.RouterProducer;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class VertxWebRecorder {

    @SuppressWarnings("unchecked")
    public Handler<RoutingContext> createHandler(String handlerClassName) {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = RouterProducer.class.getClassLoader();
            }
            Class<? extends Handler<RoutingContext>> handlerClazz = (Class<? extends Handler<RoutingContext>>) cl
                    .loadClass(handlerClassName);
            RouteHandler handler = (RouteHandler) handlerClazz.getDeclaredConstructor().newInstance();
            return handler;
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException
                | InvocationTargetException e) {
            throw new IllegalStateException("Unable to create route handler: " + handlerClassName, e);
        }
    }

    public Function<Router, io.vertx.ext.web.Route> createRouteFunction(RouteMatcher matcher,
            Handler<RoutingContext> bodyHandler) {
        return new Function<Router, io.vertx.ext.web.Route>() {
            @Override
            public io.vertx.ext.web.Route apply(Router router) {
                io.vertx.ext.web.Route route;
                if (matcher.getRegex() != null && !matcher.getRegex().isEmpty()) {
                    route = router.routeWithRegex(matcher.getRegex());
                } else if (matcher.getPath() != null && !matcher.getPath().isEmpty()) {
                    route = router.route(matcher.getPath());
                } else {
                    route = router.route();
                }
                if (matcher.getMethods().length > 0) {
                    for (String method : matcher.getMethods()) {
                        route.method(HttpMethod.valueOf(method));
                    }
                }
                if (matcher.getOrder() > 0) {
                    route.order(matcher.getOrder());
                }
                if (matcher.getProduces().length > 0) {
                    for (String produces : matcher.getProduces()) {
                        route.produces(produces);
                    }
                }
                if (matcher.getConsumes().length > 0) {
                    for (String consumes : matcher.getConsumes()) {
                        route.consumes(consumes);
                    }
                }
                if (bodyHandler != null) {
                    route.handler(bodyHandler);
                }
                return route;
            }
        };
    }

}
