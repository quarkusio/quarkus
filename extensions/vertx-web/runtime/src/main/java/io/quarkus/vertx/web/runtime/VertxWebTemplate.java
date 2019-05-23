package io.quarkus.vertx.web.runtime;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javax.enterprise.event.Event;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.Timing;
import io.quarkus.runtime.annotations.Template;
import io.quarkus.vertx.web.Route;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

@Template
public class VertxWebTemplate {

    public static void setHotReplacement(Handler<RoutingContext> handler) {
        hotReplacementHandler = handler;
    }

    private static final Logger LOGGER = Logger.getLogger(VertxWebTemplate.class.getName());

    private static volatile Handler<RoutingContext> hotReplacementHandler;

    private static volatile Router router;
    private static volatile HttpServer server;

    public void configureRouter(RuntimeValue<Vertx> vertx, BeanContainer container, Map<String, List<Route>> routeHandlers,
            VertxHttpConfiguration vertxHttpConfiguration, LaunchMode launchMode, ShutdownContext shutdown) {

        List<io.vertx.ext.web.Route> appRoutes = initialize(vertx.getValue(), vertxHttpConfiguration, routeHandlers,
                launchMode);
        container.instance(RouterProducer.class).initialize(router);

        if (launchMode == LaunchMode.DEVELOPMENT) {
            shutdown.addShutdownTask(new Runnable() {
                @Override
                public void run() {
                    for (io.vertx.ext.web.Route route : appRoutes) {
                        route.remove();
                    }
                }
            });
        }
    }

    List<io.vertx.ext.web.Route> initialize(Vertx vertx, VertxHttpConfiguration vertxHttpConfiguration,
            Map<String, List<Route>> routeHandlers,
            LaunchMode launchMode) {
        List<io.vertx.ext.web.Route> routes = new ArrayList<>();
        if (router == null) {
            router = Router.router(vertx);
            router.route().handler(BodyHandler.create());
            if (hotReplacementHandler != null) {
                router.route().blockingHandler(hotReplacementHandler);
            }
        }
        for (Entry<String, List<Route>> entry : routeHandlers.entrySet()) {
            Handler<RoutingContext> handler = createHandler(entry.getKey());
            for (Route route : entry.getValue()) {
                routes.add(addRoute(router, handler, route));
            }
        }
        // Make it also possible to register the route handlers programmatically
        Event<Object> event = Arc.container().beanManager().getEvent();
        event.select(Router.class).fire(router);

        // Start the server
        if (server == null) {
            CountDownLatch latch = new CountDownLatch(1);
            // Http server configuration
            HttpServerOptions httpServerOptions = createHttpServerOptions(vertxHttpConfiguration, launchMode);
            event.select(HttpServerOptions.class).fire(httpServerOptions);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            server = vertx.createHttpServer(httpServerOptions).requestHandler(router)
                    .listen(ar -> {
                        if (ar.succeeded()) {
                            // TODO log proper message
                            Timing.setHttpServer(String.format(
                                    "Listening on: http://%s:%s", httpServerOptions.getHost(), httpServerOptions.getPort()));

                        } else {
                            // We can't throw an exception from here as we are on the event loop.
                            // We store the failure in a reference.
                            // The reference will be checked in the main thread, and the failure re-thrown.
                            failure.set(ar.cause());
                        }
                        latch.countDown();
                    });
            try {
                latch.await();
                if (failure.get() != null) {
                    throw new IllegalStateException("Unable to start the HTTP server", failure.get());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Unable to start the HTTP server", e);
            }
        }
        return routes;
    }

    private HttpServerOptions createHttpServerOptions(VertxHttpConfiguration vertxHttpConfiguration, LaunchMode launchMode) {
        // TODO other config properties
        HttpServerOptions options = new HttpServerOptions();
        options.setHost(vertxHttpConfiguration.host);
        options.setPort(vertxHttpConfiguration.determinePort(launchMode));
        return options;
    }

    private io.vertx.ext.web.Route addRoute(Router router, Handler<RoutingContext> handler, Route routeAnnotation) {
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
