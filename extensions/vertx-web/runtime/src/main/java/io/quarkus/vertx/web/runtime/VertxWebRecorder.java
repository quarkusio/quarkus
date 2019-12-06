package io.quarkus.vertx.web.runtime;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.RouterProducer;
import io.quarkus.vertx.web.Route;
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
            return handlerClazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException
                | InvocationTargetException e) {
            throw new IllegalStateException("Unable to create invoker: " + handlerClassName, e);
        }
    }

    public Function<Router, io.vertx.ext.web.Route> createRouteFunction(Route routeAnnotation,
            Handler<RoutingContext> bodyHandler) {
        return new Function<Router, io.vertx.ext.web.Route>() {
            @Override
            public io.vertx.ext.web.Route apply(Router router) {
                io.vertx.ext.web.Route route;
                if (!routeAnnotation.regex().isEmpty()) {
                    route = router.routeWithRegex(routeAnnotation.regex());
                } else if (!routeAnnotation.path().isEmpty()) {
                    route = router.route(ensureStartWithSlash(routeAnnotation.path()));
                } else {
                    route = router.route();
                }
                if (routeAnnotation.methods().length > 0) {
                    for (HttpMethod method : routeAnnotation.methods()) {
                        route.method(method);
                    }
                }
                if (routeAnnotation.order() > 0) {
                    route.order(routeAnnotation.order());
                }
                if (routeAnnotation.produces().length > 0) {
                    for (String produces : routeAnnotation.produces()) {
                        route.produces(produces);
                    }
                }
                if (routeAnnotation.consumes().length > 0) {
                    for (String consumes : routeAnnotation.consumes()) {
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

    private String ensureStartWithSlash(String path) {
        if (path.startsWith("/")) {
            return path;
        } else {
            return "/" + path;
        }
    }

}
