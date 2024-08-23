package io.quarkus.vertx.http.deployment.spi;

import java.util.OptionalInt;
import java.util.function.Consumer;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.builder.item.MultiBuildItem;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

/**
 * A build item that represents a route that should be added to the router.
 * <p>
 * Producing this build item does not mean the HTTP server is available.
 * It will be consumed if the Quarkus Vert.x HTTP extension is present.
 */
public final class RouteBuildItem extends MultiBuildItem {

    /**
     * The type of route handler
     */
    public enum HandlerType {

        /**
         * A regular route handler invoked on the event loop.
         *
         * @see io.vertx.ext.web.Route#handler(Handler)
         */
        NORMAL,
        /**
         * A blocking route handler, invoked on a worker thread.
         *
         * @see io.vertx.ext.web.Route#blockingHandler(Handler)
         */
        BLOCKING,
        /**
         * A failure handler, invoked when an exception is thrown from a route handler.
         * This is invoked on the event loop.
         *
         * @see io.vertx.ext.web.Route#failureHandler(Handler)
         */
        FAILURE

    }

    /**
     * Type of routes.
     */
    public enum RouteType {

        /**
         * Framework routes are provided by the Quarkus framework (or extensions).
         * They are not related to the application business logic, but provide a non-functional feature (health, metrics...).
         * <p>
         * Framework route can be mounted on the application router (under the non application route path) or on the management
         * router when enabled.
         */
        FRAMEWORK_ROUTE,
        /**
         * Application routes are part of the application business logic.
         * They are mounted on the application router (so the application prefix is applied).
         */
        APPLICATION_ROUTE,
        /**
         * Absolute routes are part of the application business logic, and are mounted on the root router (exposed on /).
         */
        ABSOLUTE_ROUTE
    }

    private final RouteType typeOfRoute;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final OptionalInt order;

    private final String path;
    private final Consumer<Route> customizer;

    private final boolean isManagement;

    private final Handler<RoutingContext> handler;

    private final HandlerType typeOfHandler = HandlerType.NORMAL;

    private final boolean displayOnNotFoundPage;
    private final String notFoundPageTitle;

    private final String routeConfigKey;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public RouteBuildItem(RouteType typeOfRoute, String path, Consumer<Route> customizer,
            boolean isManagement,
            Handler<RoutingContext> handler,
            boolean displayOnNotFoundPage,
            String notFoundPageTitle,
            String routeConfigKey, OptionalInt order) {
        this.order = order;
        this.typeOfRoute = typeOfRoute;
        this.path = path;
        this.handler = handler;
        this.displayOnNotFoundPage = displayOnNotFoundPage;
        this.notFoundPageTitle = notFoundPageTitle;
        this.routeConfigKey = routeConfigKey;
        this.customizer = customizer;
        this.isManagement = isManagement;
    }

    public RouteType getTypeOfRoute() {
        return typeOfRoute;
    }

    public boolean hasOrder() {
        return order.isPresent();
    }

    public int getOrder() {
        if (order.isPresent()) {
            return order.getAsInt();
        } else {
            throw new IllegalStateException("No order set");
        }
    }

    public boolean hasRouteConfigKey() {
        return routeConfigKey != null;
    }

    public String getRouteConfigKey() {
        return routeConfigKey;
    }

    public Handler<RoutingContext> getHandler() {
        return handler;
    }

    public HandlerType getHandlerType() {
        return typeOfHandler;
    }

    public String getPath() {
        return path;
    }

    public Consumer<Route> getCustomizer() {
        return customizer;
    }

    public String getNotFoundPageTitle() {
        return notFoundPageTitle;
    }

    public boolean isDisplayOnNotFoundPage() {
        return displayOnNotFoundPage;
    }

    /**
     * Declares a new application route.
     * Application routes are part of the application business logic and are mounted on the application router.
     * The {@code quarkus.http.root-path} property is applied in front of the route path (if set).
     *
     * @param path the path, must not be {@code null} or empty
     * @return the builder to configure the route
     */
    public static Builder newApplicationRoute(String path) {
        return new Builder(RouteType.APPLICATION_ROUTE, path, false);
    }

    /**
     * Declares a new absolute route.
     * Application routes are part of the application business logic and are mounted at the root of the server.
     * The {@code quarkus.http.root-path} property is <em>not</em> applied.
     *
     * @param path the path, must not be {@code null} or empty, and must start with a slash
     * @return the builder to configure the route
     */
    public static Builder newAbsoluteRoute(String path) {
        return new Builder(RouteType.ABSOLUTE_ROUTE, path, false);
    }

    /**
     * Declares a new framework route.
     * A framework route is provided by the Quarkus framework (or extensions).
     * <p>
     * The {@code quarkus.http.non-application-root-path} property is applied in front of the route path (defaults to
     * {@code /q}).
     * <p>
     * The declared route is not considered as a management route, meaning it will be mounted on the application router
     * and exposed on the main HTTP server. See {@link #newManagementRoute(String)} to declare a management route.
     *
     * @param path the path, must not be {@code null} or empty.
     * @return the builder to configure the route
     */
    public static Builder newFrameworkRoute(String path) {
        return new Builder(RouteType.FRAMEWORK_ROUTE, path, false);
    }

    /**
     * Declares a new management route.
     * <p>
     * A management route is provided by the Quarkus framework (or extensions), and unlike routes declared with
     * {@link #newFrameworkRoute(String)},
     * are mounted on the management router (exposed on the management HTTP server) when the management interface is
     * enabled (see <a href="https://quarkus.io/guides/management-interface-reference">the management interface
     * documentation</a> for further details).
     * <p>
     * If the management interface is not enabled, the {@code quarkus.http.non-application-root-path} property is applied in
     * front of the route path (defaults to {@code /q}).
     * If the management interface is enabled, the {@code quarkus.management.root-path} property is applied in front of the
     * route path (also defaults to {@code /q} but exposed on another port, 9000 by default).
     *
     * @param path the path, must not be {@code null} or empty.
     * @return the builder to configure the route
     */
    public static Builder newManagementRoute(String path) {
        return new Builder(RouteType.FRAMEWORK_ROUTE, path, true);
    }

    /**
     * Declares a new framework route, conditionally considered as a management route depending on the value of the
     * {@code managementConfigKey} property.
     *
     * <p>
     * The route is provided by the Quarkus framework (or extensions). Depending on the value associated to the
     * {@code managementConfigKey} property,
     * the route is either mounted to the application router (exposed on the main HTTP server) or on the management router
     * (exposed on the management HTTP server).
     * The property must be a boolean (set to {@code true} to expose the route on the management server or {@code false} to
     * expose it on the main HTTP server).
     * <p>
     * If the management interface is not enabled, regardless the value of the property, the route is exposed on the main HTTP
     * server.
     * The {@code quarkus.http.non-application-root-path} property is applied in front of the route path (defaults to
     * {@code /q}).
     * <p>
     * If the management interface is enabled and if the property is set to {@code true}, the route is exposed on the management
     * server and the {@code quarkus.management.root-path} property is applied in front of the route path (also defaults to
     * {@code /q} but exposed on another port, 9000 by default).
     * <p>
     * If the management interface is enabled and if the property is set to {@code false}, the route is exposed on the main HTTP
     * server.
     * The {@code quarkus.http.non-application-root-path} property is applied in front of the route path (defaults to
     * {@code /q}).
     *
     * @param path the path, must not be {@code null} or empty.
     * @return the builder to configure the route
     */
    public static Builder newManagementRoute(String path, String managementConfigKey) {
        return new Builder(RouteType.FRAMEWORK_ROUTE, path,
                (managementConfigKey == null || isManagement(managementConfigKey)));
    }

    private static boolean isManagement(String managementConfigKey) {
        Config config = ConfigProvider.getConfig();
        return config.getValue(managementConfigKey, boolean.class);
    }

    public boolean isManagement() {
        return isManagement;
    }

    /**
     * A builder to configure the route.
     */
    public static class Builder {

        private final RouteType typeOfRoute;
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private OptionalInt order = OptionalInt.empty();

        private final String path;
        private final boolean isManagement;
        private Consumer<Route> customizer;

        private Handler<RoutingContext> handler;

        private HandlerType typeOfHandler = HandlerType.NORMAL;

        private boolean displayOnNotFoundPage;
        private String notFoundPageTitle;

        private String routeConfigKey;

        private Builder(RouteType type, String path, boolean isManagement) {
            this.typeOfRoute = type;
            this.path = path;
            this.isManagement = isManagement;
        }

        /**
         * Sets a function to customize the route.
         *
         * @param customizer the customizer, must not be {@code null}
         * @return the current builder
         */
        public Builder withRouteCustomizer(Consumer<Route> customizer) {
            this.customizer = customizer;
            return this;
        }

        /**
         * Defines the route order.
         *
         * @param order the order
         * @return the current builder
         */
        public Builder withOrder(int order) {
            this.order = OptionalInt.of(order);
            return this;
        }

        /**
         * Sets the request handler (mandatory)
         *
         * @param handler the handler, must not be {@code null}
         * @return the current builder
         */
        public Builder withRequestHandler(Handler<RoutingContext> handler) {
            this.handler = handler;
            return this;
        }

        /**
         * Sets the route as a blocking route.
         * A blocking route handler is invoked on a worker thread, and thus is allowed to block.
         *
         * @return the current builder
         */
        public Builder asBlockingRoute() {
            if (this.typeOfHandler == HandlerType.FAILURE) {
                throw new IllegalArgumentException("A failure route cannot be a blocking route");
            }
            this.typeOfHandler = HandlerType.BLOCKING;
            return this;
        }

        /**
         * Sets the route as a failure route.
         * A failure route handler is invoked when an exception is thrown from a route handler.
         *
         * @return the current builder
         */
        public Builder asFailureRoute() {
            if (this.typeOfHandler == HandlerType.BLOCKING) {
                throw new IllegalArgumentException("A blocking route cannot be a failure route");
            }
            this.typeOfHandler = HandlerType.FAILURE;
            return this;
        }

        /**
         * Adds the route to the page returned when a 404 error is returned.
         *
         * @return the current builder
         */
        public Builder displayOnNotFoundPage() {
            this.displayOnNotFoundPage = true;
            return this;
        }

        /**
         * Adds the route to the page returned when a 404 error is returned, and sets the title of the page.
         *
         * @param notFoundPageTitle the title of the route
         * @return the current builder
         */
        public Builder displayOnNotFoundPage(String notFoundPageTitle) {
            this.displayOnNotFoundPage = true;
            this.notFoundPageTitle = notFoundPageTitle;
            return this;
        }

        /**
         * Sets a property configuring the route path.
         *
         * @param attributeName the name of the property configuring the route path
         * @return the current builder
         */
        public Builder withRoutePathConfigKey(String attributeName) {
            this.routeConfigKey = attributeName;
            return this;
        }

        /**
         * Validates the route and build the {@code RouteBuildItem}.
         *
         * @return the route build item
         */
        public RouteBuildItem build() {
            if (this.handler == null) {
                throw new IllegalArgumentException("The route handler must be set");
            }

            return new RouteBuildItem(typeOfRoute, path, customizer, isManagement, handler, displayOnNotFoundPage,
                    notFoundPageTitle,
                    routeConfigKey, order);
        }
    }

}
