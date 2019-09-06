package io.quarkus.vertx.web.runtime;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.enterprise.event.Event;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.RouterProducer;
import io.quarkus.vertx.web.Route;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

@Recorder
public class VertxWebRecorder {

    private static final Logger LOGGER = Logger.getLogger(VertxWebRecorder.class.getName());

    public void addAdditionalRoutes(RuntimeValue<Router> routerRuntimeValue, Map<String, List<Route>> routeHandlers,
            List<Handler<RoutingContext>> filters) {
        Router router = routerRuntimeValue.getValue();

        for (Entry<String, List<Route>> entry : routeHandlers.entrySet()) {
            Handler<RoutingContext> handler = createHandler(entry.getKey());
            for (Route route : entry.getValue()) {
                addRoute(router, handler, route, filters);
            }
        }
        // Make it also possible to register the route handlers programmatically
        Event<Object> event = Arc.container().beanManager().getEvent();
        event.select(Router.class).fire(router);
    }

    private io.vertx.ext.web.Route addRoute(Router router, Handler<RoutingContext> handler, Route routeAnnotation,
            List<Handler<RoutingContext>> filters) {
        io.vertx.ext.web.Route route;
        if (!routeAnnotation.regex().isEmpty()) {
            route = router.routeWithRegex(routeAnnotation.regex());
        } else if (!routeAnnotation.path().isEmpty()) {
            route = router.route(routeAnnotation.path());
        } else {
            route = router.route();
        }
        if (routeAnnotation.methods().length > 0) {
            for (HttpMethod method : routeAnnotation.methods()) {
                route.method(method);
            }
        }
        if (routeAnnotation.order() != Integer.MIN_VALUE) {
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

        // we add the filters to each route
        for (Handler<RoutingContext> i : filters) {
            if (i != null) {
                route.handler(i);
            }
        }

        route.handler(BodyHandler.create());
        switch (routeAnnotation.type()) {
            case NORMAL:
                route.handler(handler);
                break;
            case BLOCKING:
                // We don't mind if blocking handlers are executed in parallel
                route.blockingHandler(handler, false);
                break;
            case FAILURE:
                route.failureHandler(handler);
                break;
            default:
                throw new IllegalStateException("Unsupported handler type: " + routeAnnotation.type());
        }
        LOGGER.debugf("Route registered for %s", routeAnnotation);
        return route;
    }

    @SuppressWarnings("unchecked")
    private Handler<RoutingContext> createHandler(String handlerClassName) {
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
}
