package io.quarkus.vertx.http.deployment;

import static io.quarkus.vertx.http.deployment.RouteBuildItem.RouteType.APPLICATION_ROUTE;

import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.vertx.http.deployment.devmode.ConfiguredPathInfo;
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

    private final boolean management;

    private final Function<Router, Route> routeFunction;
    private final Handler<RoutingContext> handler;
    private final HandlerType type;
    private final RouteType routeType;
    private final RouteType routerType;
    private final NotFoundPageDisplayableEndpointBuildItem notFoundPageDisplayableEndpoint;
    private final String absolutePath;
    private final ConfiguredPathInfo configuredPathInfo;

    RouteBuildItem(Builder builder, RouteType routeType, RouteType routerType, boolean management) {
        this.routeFunction = builder.routeFunction;
        this.handler = builder.handler;
        this.management = management;
        this.type = builder.type;
        this.routeType = routeType;
        this.routerType = routerType;
        this.notFoundPageDisplayableEndpoint = builder.getNotFoundEndpoint();
        this.configuredPathInfo = builder.getRouteConfigInfo();
        this.absolutePath = builder.absolutePath;
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

    public RouteType getRouteType() {
        return routeType;
    }

    public RouteType getRouterType() {
        return routerType;
    }

    public boolean isRouterFramework() {
        return routerType.equals(RouteType.FRAMEWORK_ROUTE);
    }

    public boolean isRouterApplication() {
        return routerType.equals(APPLICATION_ROUTE);
    }

    public boolean isRouterAbsolute() {
        return routerType.equals(RouteType.ABSOLUTE_ROUTE);
    }

    public NotFoundPageDisplayableEndpointBuildItem getNotFoundPageDisplayableEndpoint() {
        return notFoundPageDisplayableEndpoint;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public ConfiguredPathInfo getConfiguredPathInfo() {
        return configuredPathInfo;
    }

    /**
     * @return {@code true} if the route is exposing a management endpoint.
     *         It matters when using a different interface/port for the management endpoints, as these routes will only
     *         be accessible from that different interface/port.
     */
    public boolean isManagement() {
        return management;
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

        protected boolean isManagement;

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

        /**
         * @param route A normalized path used to define a basic route
         *        (e.g. use HttpRootPathBuildItem to construct/resolve the path value). This path this is also
         *        used on the "Not Found" page in dev mode.
         * @param order Priority ordering of the route
         * @param routeCustomizer Route customizer.
         */
        public Builder orderedRoute(String route, Integer order, Consumer<Route> routeCustomizer) {
            this.routeFunction = new BasicRoute(route, order, routeCustomizer);
            this.notFoundPagePath = this.routePath = route;
            return this;
        }

        /**
         * @param name The name of the route. It is used to identify the route in the metrics.
         * @param route A normalized path used to define a basic route
         *        (e.g. use HttpRootPathBuildItem to construct/resolve the path value). This path this is also
         *        used on the "Not Found" page in dev mode.
         * @param order Priority ordering of the route
         * @param routeCustomizer Route customizer.
         */
        public Builder orderedRoute(String name, String route, Integer order, Consumer<Route> routeCustomizer) {
            this.routeFunction = new BasicRoute(name, route, order, routeCustomizer);
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

        public Builder management() {
            return management(null);
        }

        public Builder management(String managementConfigKey) {
            if (managementConfigKey == null || shouldInclude(managementConfigKey)) {
                this.isManagement = true;
            } else {
                this.isManagement = false;
            }
            return this;
        }

        private boolean shouldInclude(String managementConfigKey) {
            Config config = ConfigProvider.getConfig();
            return config.getValue(managementConfigKey, boolean.class);
        }

        public RouteBuildItem build() {
            if (routeFunction == null) {
                throw new IllegalStateException(
                        "'RouteBuildItem$Builder.routeFunction' was not set. Ensure that one of the builder methods that result in it being set is called");
            }
            return new RouteBuildItem(this, APPLICATION_ROUTE, APPLICATION_ROUTE, isManagement);
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
                return new ConfiguredPathInfo(routeConfigKey, absolutePath, true, isManagement);
            }
            return new ConfiguredPathInfo(routeConfigKey, routePath, false, isManagement);
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
