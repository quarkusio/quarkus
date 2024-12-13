package io.quarkus.vertx.web.runtime;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.HttpCompression;
import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.quarkus.vertx.http.runtime.security.HttpSecurityRecorder.DefaultAuthFailureHandler;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class VertxWebRecorder {

    final RuntimeValue<VertxHttpConfig> httpConfig;
    final VertxHttpBuildTimeConfig httpBuildTimeConfig;

    public VertxWebRecorder(RuntimeValue<VertxHttpConfig> httpConfig,
            VertxHttpBuildTimeConfig httpBuildTimeConfig) {
        this.httpConfig = httpConfig;
        this.httpBuildTimeConfig = httpBuildTimeConfig;
    }

    @SuppressWarnings("unchecked")
    public Handler<RoutingContext> createHandler(String handlerClassName) {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = VertxWebRecorder.class.getClassLoader();
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

    public Handler<RoutingContext> runOnVirtualThread(Handler<RoutingContext> routeHandler) {
        return new VirtualThreadsRouteHandler(routeHandler);
    }

    public Handler<RoutingContext> compressRouteHandler(Handler<RoutingContext> routeHandler, HttpCompression compression) {
        if (httpBuildTimeConfig.enableCompression()) {
            return new HttpCompressionHandler(routeHandler, compression,
                    compression == HttpCompression.UNDEFINED
                            ? Set.copyOf(httpBuildTimeConfig.compressMediaTypes().orElse(List.of()))
                            : Set.of());
        } else {
            return routeHandler;
        }
    }

    public Function<Router, io.vertx.ext.web.Route> createRouteFunction(RouteMatcher matcher,
            Handler<RoutingContext> bodyHandler, boolean alwaysAuthenticateRoute) {
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
                if (alwaysAuthenticateRoute) {
                    route = route.handler(routingContext -> {
                        // check auth haven't happened further up the handler chain
                        if (routingContext.user() == null) {
                            // authenticate -> on deferred identity (Uni's) termination user is set to the routing context,
                            // so SecurityIdentity will be accessible in a synchronous manner
                            routingContext.<Uni<SecurityIdentity>> get(QuarkusHttpUser.DEFERRED_IDENTITY_KEY)
                                    .subscribe().withSubscriber(new UniSubscriber<Object>() {
                                        @Override
                                        public void onSubscribe(UniSubscription subscription) {
                                        }

                                        @Override
                                        public void onItem(Object item) {
                                            if (routingContext.response().ended()) {
                                                return;
                                            }
                                            routingContext.next();
                                        }

                                        @Override
                                        public void onFailure(Throwable failure) {
                                            BiConsumer<RoutingContext, Throwable> handler = routingContext
                                                    .get(QuarkusHttpUser.AUTH_FAILURE_HANDLER);
                                            if (handler != null) {
                                                handler.accept(routingContext, failure);
                                            }
                                        }
                                    });
                        } else {
                            routingContext.next();
                        }
                    });
                }
                if (bodyHandler != null) {
                    route.handler(bodyHandler);
                }
                return route;
            }
        };
    }

    public Handler<RoutingContext> addAuthFailureHandler() {
        return new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                if (event.get(QuarkusHttpUser.AUTH_FAILURE_HANDLER) instanceof DefaultAuthFailureHandler) {
                    // failing event rather than end it makes it possible to customize response
                    // QuarkusErrorHandler will send response if the failure is not handled elsewhere
                    event.put(QuarkusHttpUser.AUTH_FAILURE_HANDLER, new DefaultAuthFailureHandler() {
                        @Override
                        protected void proceed(Throwable throwable) {

                            if (!event.failed()) {
                                event.fail(throwable);
                            }
                        }
                    });
                }
                event.next();
            }
        };
    }

}
