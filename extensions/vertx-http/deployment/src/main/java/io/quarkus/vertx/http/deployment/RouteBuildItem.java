package io.quarkus.vertx.http.deployment;

import java.util.function.Consumer;
import java.util.function.Function;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.quarkus.vertx.http.deployment.devmode.console.ConfiguredPathInfo;
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
    private final NotFoundPageDisplayableEndpointBuildItem notFoundPageDisplayableEndpoint;
    private final ConfiguredPathInfo devConsoleResolvedPathBuildItem;

    RouteBuildItem(Builder builder, RouteType routeType) {
        this.routeFunction = builder.routeFunction;
        this.handler = builder.handler;
        this.type = builder.type;
        this.routeType = routeType;
        this.notFoundPageDisplayableEndpoint = builder.getNotFoundEndpoint();
        this.devConsoleResolvedPathBuildItem = builder.getRouteConfigInfo();
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

    public NotFoundPageDisplayableEndpointBuildItem getNotFoundPageDisplayableEndpoint() {
        return notFoundPageDisplayableEndpoint;
    }

    public ConfiguredPathInfo getDevConsoleResolvedPath() {
        return devConsoleResolvedPathBuildItem;
    }

    public enum RouteType {
        FRAMEWORK_ROUTE,
        APPLICATION_ROUTE,
        ABSOLUTE_ROUTE
    }

    /**
     * HttpRootPathBuildItem.Builder and NonApplicationRootPathBuildItem.Builder extend this.
     * Please verify the extended builders behavior when changing this one.
     */
    public static class Builder {
        protected Function<Router, Route> routeFunction;
        protected Handler<RoutingContext> handler;
        protected HandlerType type = HandlerType.NORMAL;
        protected boolean displayOnNotFoundPage;
        protected String notFoundPageTitle;
        protected String notFoundPagePath;
        protected String routePath;
        protected String routeConfigKey;
        protected String absolutePath;

        /**
         * {@link #routeFunction(String, Consumer)} should be used instead
         *
         * @param routeFunction
         * @see #routeFunction(String, Consumer)
         */
        @Deprecated
        public Builder routeFunction(Function<Router, Route> routeFunction) {
            this.routeFunction = routeFunction;
            return this;
        }

        /**
         * @param path A normalized path (e.g. use HttpRootPathBuildItem to construct/resolve the path value) defining
         *        the route. This path this is also used on the "Not Found" page in dev mode.
         * @param routeFunction a Consumer of Route
         */
        public Builder routeFunction(String path, Consumer<Route> routeFunction) {
            this.routeFunction = new BasicRoute(path, null, routeFunction);
            this.notFoundPagePath = this.routePath = path;
            return this;
        }

        /**
         * @param route A normalized path used to define a basic route
         *        (e.g. use HttpRootPathBuildItem to construct/resolve the path value). This path this is also
         *        used on the "Not Found" page in dev mode.
         */
        public Builder route(String route) {
            this.routeFunction = new BasicRoute(route);
            this.notFoundPagePath = this.routePath = route;
            return this;
        }

        /**
         * @param route A normalized path used to define a basic route
         *        (e.g. use HttpRootPathBuildItem to construct/resolve the path value). This path this is also
         *        used on the "Not Found" page in dev mode.
         * @param order Priority ordering of the route
         */
        public Builder orderedRoute(String route, Integer order) {
            this.routeFunction = new BasicRoute(route, order);
            this.notFoundPagePath = this.routePath = route;
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

        public Builder routeConfigKey(String attributeName) {
            this.routeConfigKey = attributeName;
            return this;
        }

        public RouteBuildItem build() {
            return new RouteBuildItem(this, RouteType.APPLICATION_ROUTE);
        }

        protected ConfiguredPathInfo getRouteConfigInfo() {
            if (routeConfigKey == null) {
                return null;
            }
            if (routePath == null) {
                throw new RuntimeException("Cannot discover value of " + routeConfigKey
                        + " as no explicit path was specified and a route function is in use");
            }
            if (absolutePath != null) {
                return new ConfiguredPathInfo(routeConfigKey, absolutePath, true);
            }
            return new ConfiguredPathInfo(routeConfigKey, routePath, false);
        }

        protected NotFoundPageDisplayableEndpointBuildItem getNotFoundEndpoint() {
            if (!displayOnNotFoundPage) {
                return null;
            }
            if (notFoundPagePath == null) {
                throw new RuntimeException("Cannot display " + routeFunction
                        + " on not found page as no explicit path was specified and a route function is in use");
            }
            if (absolutePath != null) {
                return new NotFoundPageDisplayableEndpointBuildItem(absolutePath, notFoundPageTitle, true);
            }
            return new NotFoundPageDisplayableEndpointBuildItem(notFoundPagePath, notFoundPageTitle, false);
        }
    }
}
