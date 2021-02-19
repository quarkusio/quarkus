package io.quarkus.vertx.http.deployment;

import java.util.function.Consumer;
import java.util.function.Function;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.quarkus.vertx.http.runtime.BasicRoute;
import io.quarkus.vertx.http.runtime.HandlerType;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public final class RouteBuildItem extends MultiBuildItem {

    public static Builder builder() {
        return new Builder();
    }

    private final Function<Router, Route> routeFunction;
    private final Handler<RoutingContext> handler;
    private final HandlerType type;
    private final RouteType routeType;
    private final boolean requiresLegacyRedirect;
    private final NotFoundPageDisplayableEndpointBuildItem notFoundPageDisplayableEndpoint;

    /**
     * @deprecated Use the Builder instead.
     */
    @Deprecated
    public RouteBuildItem(Function<Router, Route> routeFunction, Handler<RoutingContext> handler, HandlerType type) {
        this.routeFunction = routeFunction;
        this.handler = handler;
        this.type = type;
        this.routeType = RouteType.APPLICATION_ROUTE;
        this.requiresLegacyRedirect = false;
        this.notFoundPageDisplayableEndpoint = null;
    }

    /**
     * @deprecated Use the Builder instead.
     */
    @Deprecated
    public RouteBuildItem(Function<Router, Route> routeFunction, Handler<RoutingContext> handler) {
        this(routeFunction, handler, HandlerType.NORMAL);
    }

    /**
     * @deprecated Use the Builder instead.
     */
    @Deprecated
    public RouteBuildItem(String route, Handler<RoutingContext> handler, HandlerType type, boolean resume) {
        this(new BasicRoute(route), handler, type);
    }

    /**
     * @deprecated Use the Builder instead.
     */
    @Deprecated
    public RouteBuildItem(String route, Handler<RoutingContext> handler, HandlerType type) {
        this(new BasicRoute(route), handler, type);
    }

    /**
     * @deprecated Use the Builder instead.
     */
    @Deprecated
    public RouteBuildItem(String route, Handler<RoutingContext> handler, boolean resume) {
        this(new BasicRoute(route), handler, HandlerType.NORMAL);
    }

    /**
     * @deprecated Use the Builder instead.
     */
    @Deprecated
    public RouteBuildItem(String route, Handler<RoutingContext> handler) {
        this(new BasicRoute(route), handler);
    }

    RouteBuildItem(Builder builder, RouteType routeType, boolean requiresLegacyRedirect) {
        this.routeFunction = builder.routeFunction;
        this.handler = builder.handler;
        this.type = builder.type;
        this.routeType = routeType;
        this.requiresLegacyRedirect = requiresLegacyRedirect;
        this.notFoundPageDisplayableEndpoint = builder.getNotFoundEndpoint();
    }

    public Handler<RoutingContext> getHandler() {
        return handler;
    }

    public HandlerType getType() {
        return type;
    }

    public Function<Router, Route> getRouteFunction() {
        return routeFunction;
    }

    public boolean isFrameworkRoute() {
        return routeType.equals(RouteType.FRAMEWORK_ROUTE);
    }

    public boolean isApplicationRoute() {
        return routeType.equals(RouteType.APPLICATION_ROUTE);
    }

    public boolean isAbsoluteRoute() {
        return routeType.equals(RouteType.ABSOLUTE_ROUTE);
    }

    public boolean isRequiresLegacyRedirect() {
        return requiresLegacyRedirect;
    }

    public NotFoundPageDisplayableEndpointBuildItem getNotFoundPageDisplayableEndpoint() {
        return notFoundPageDisplayableEndpoint;
    }

    public enum RouteType {
        FRAMEWORK_ROUTE,
        APPLICATION_ROUTE,
        ABSOLUTE_ROUTE
    }

    /**
     * NonApplicationRootPathBuildItem.Builder extends this.
     * Please verify the extended builders behavior when changing this one.
     */
    public static class Builder {
        protected Function<Router, Route> routeFunction;
        protected Handler<RoutingContext> handler;
        protected HandlerType type = HandlerType.NORMAL;
        protected boolean displayOnNotFoundPage;
        protected String notFoundPageTitle;
        protected String notFoundPagePath;

        /**
         * {@link #routeFunction(String, Consumer)} should be used instead
         *
         * @param routeFunction
         * @return
         */
        @Deprecated
        public Builder routeFunction(Function<Router, Route> routeFunction) {
            this.routeFunction = routeFunction;
            return this;
        }

        public Builder routeFunction(String path, Consumer<Route> routeFunction) {
            this.routeFunction = new BasicRoute(path, null, routeFunction);
            this.notFoundPagePath = path;
            return this;
        }

        public Builder route(String route) {
            this.routeFunction = new BasicRoute(route);
            this.notFoundPagePath = route;
            return this;
        }

        public Builder handler(Handler<RoutingContext> handler) {
            this.handler = handler;
            return this;
        }

        public Builder handlerType(HandlerType handlerType) {
            this.type = handlerType;
            return this;
        }

        public Builder blockingRoute() {
            this.type = HandlerType.BLOCKING;
            return this;
        }

        public Builder failureRoute() {
            this.type = HandlerType.FAILURE;
            return this;
        }

        public Builder displayOnNotFoundPage() {
            this.displayOnNotFoundPage = true;
            return this;
        }

        public Builder displayOnNotFoundPage(String notFoundPageTitle) {
            this.displayOnNotFoundPage = true;
            this.notFoundPageTitle = notFoundPageTitle;
            return this;
        }

        public Builder displayOnNotFoundPage(String notFoundPageTitle, String notFoundPagePath) {
            this.displayOnNotFoundPage = true;
            this.notFoundPageTitle = notFoundPageTitle;
            this.notFoundPagePath = notFoundPagePath;
            return this;
        }

        public RouteBuildItem build() {
            return new RouteBuildItem(this, RouteType.APPLICATION_ROUTE, false);
        }

        protected NotFoundPageDisplayableEndpointBuildItem getNotFoundEndpoint() {
            if (!displayOnNotFoundPage) {
                return null;
            }
            if (notFoundPagePath == null) {
                throw new RuntimeException("Cannot display " + routeFunction
                        + " on not found page as no explicit path was specified and a route function is in use");
            }
            return new NotFoundPageDisplayableEndpointBuildItem(notFoundPagePath, notFoundPageTitle);
        }
    }
}
