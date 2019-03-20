package io.quarkus.vertx.web.runtime;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.Timing;
import io.quarkus.vertx.web.Route;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * NOTE: The HTTP server instance is closed within {@link io.quarkus.vertx.runtime.VertxProducer#destroy()}.
 */
@Singleton
public class HttpServerInitializer {

    public static void setHotReplacement(Handler<RoutingContext> handler) {
        hotReplacementHandler = handler;
    }

    private static final Logger LOGGER = Logger.getLogger(HttpServerInitializer.class.getName());

    private static volatile Handler<RoutingContext> hotReplacementHandler;

    private volatile Router router;
    private volatile io.vertx.reactivex.ext.web.Router rxRouter;

    @Inject
    Vertx vertx;

    @Inject
    Event<Router> routerEvent;

    @Inject
    Event<HttpServerOptions> serverOptionsEvent;

    void initialize(VertxHttpConfiguration vertxHttpConfiguration, Map<String, List<Route>> routeHandlers,
            LaunchMode launchMode) {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        if (hotReplacementHandler != null) {
            router.route().blockingHandler(hotReplacementHandler);
        }
        for (Entry<String, List<Route>> entry : routeHandlers.entrySet()) {
            Handler<RoutingContext> handler = createHandler(entry.getKey());
            for (Route route : entry.getValue()) {
                addRoute(router, handler, route);
            }
        }
        // Make it also possible to register the route handlers programatically
        routerEvent.fire(router);
        this.router = router;
        this.rxRouter = io.vertx.reactivex.ext.web.Router.newInstance(router);
        // Http server configuration
        HttpServerOptions httpServerOptions = createHttpServerOptions(vertxHttpConfiguration, launchMode);
        serverOptionsEvent.fire(httpServerOptions);
        // Start the server
        CountDownLatch latch = new CountDownLatch(1);
        vertx.createHttpServer(httpServerOptions).requestHandler(router)
                .listen(ar -> {
                    if (ar.succeeded()) {
                        // TODO log proper message
                        Timing.setHttpServer(String.format(
                                "Listening on: http://%s:%s", httpServerOptions.getHost(), httpServerOptions.getPort()));
                        latch.countDown();
                    }
                });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Unable to start the HTTP server", e);
        }
    }

    // Note that we need a client proxy because if a bean also @Observes Router a null value would be injected 
    @ApplicationScoped
    @Produces
    Router produceRouter() {
        return router;
    }

    @ApplicationScoped
    @Produces
    io.vertx.reactivex.ext.web.Router produceRxRouter() {
        return rxRouter;
    }

    private HttpServerOptions createHttpServerOptions(VertxHttpConfiguration vertxHttpConfiguration, LaunchMode launchMode) {
        // TODO other config properties
        HttpServerOptions options = new HttpServerOptions();
        options.setHost(vertxHttpConfiguration.host);
        options.setPort(vertxHttpConfiguration.determinePort(launchMode));
        return options;
    }

    private void addRoute(Router router, Handler<RoutingContext> handler, Route routeAnnotation) {
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
    }

    @SuppressWarnings("unchecked")
    private Handler<RoutingContext> createHandler(String handlerClassName) {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = HttpServerInitializer.class.getClassLoader();
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
