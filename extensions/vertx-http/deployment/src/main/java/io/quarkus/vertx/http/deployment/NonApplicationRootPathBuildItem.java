package io.quarkus.vertx.http.deployment;

import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Function;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.util.UriNormalizationUtil;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.quarkus.vertx.http.deployment.devmode.console.ConfiguredPathInfo;
import io.quarkus.vertx.http.runtime.HandlerType;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public final class NonApplicationRootPathBuildItem extends SimpleBuildItem {
    /**
     * Normalized of quarkus.http.root-path.
     * Must end in a slash
     */
    final URI httpRootPath;

    /**
     * Normalized from quarkus.http.non-application-root-path
     */
    final URI nonApplicationRootPath;

    /**
     * Non-Application root path is distinct from HTTP root path.
     */
    final boolean dedicatedRouterRequired;

    final boolean attachedToMainRouter;

    public NonApplicationRootPathBuildItem(String httpRootPath, String nonApplicationRootPath) {
        // Presume value always starts with a slash and is normalized
        this.httpRootPath = UriNormalizationUtil.toURI(httpRootPath, true);

        this.nonApplicationRootPath = UriNormalizationUtil.normalizeWithBase(this.httpRootPath, nonApplicationRootPath,
                true);

        this.dedicatedRouterRequired = !this.nonApplicationRootPath.getPath().equals(this.httpRootPath.getPath());

        // Is the non-application root path underneath the http root path. Do we add non-application root to main router or not.
        this.attachedToMainRouter = this.nonApplicationRootPath.getPath().startsWith(this.httpRootPath.getPath());
    }

    /**
     * Is a dedicated router required for non-application endpoints.
     *
     * @return boolean
     */
    public boolean isDedicatedRouterRequired() {
        return dedicatedRouterRequired;
    }

    public boolean isAttachedToMainRouter() {
        return attachedToMainRouter;
    }

    /**
     * Path to the Non-application root for use with Vert.x Routers,
     * has a leading slash.
     * <p>
     * If it's under the HTTP Root, return a path relative to HTTP Root.
     * Otherwise, return an absolute path.
     *
     * @return String Path suitable for use with Vert.x Router. It has a leading slash
     */
    String getVertxRouterPath() {
        if (attachedToMainRouter) {
            return "/" + UriNormalizationUtil.relativize(httpRootPath.getPath(), nonApplicationRootPath.getPath());
        } else {
            return getNonApplicationRootPath();
        }
    }

    public String getNormalizedHttpRootPath() {
        return httpRootPath.getPath();
    }

    /**
     * Return normalized root path configured from {@literal quarkus.http.root-path}
     * and {quarkus.http.non-application-root-path}.
     * This path will always end in a slash.
     * <p>
     * Use {@link #resolvePath(String)} if you need to construct a URI for
     * a non-application endpoint.
     *
     * @return Normalized non-application root path ending with a slash
     * @see #resolvePath(String)
     */
    public String getNonApplicationRootPath() {
        return nonApplicationRootPath.getPath();
    }

    /**
     * Resolve path into an absolute path.
     * If path is relative, it will be resolved against `quarkus.http.non-application-root-path`.
     * An absolute path will be normalized and returned.
     * <p>
     * Given {@literal quarkus.http.root-path=/} and
     * {@literal quarkus.http.non-application-root-path="q"}
     * <ul>
     * <li>{@code resolvePath("foo")} will return {@literal /q/foo}</li>
     * <li>{@code resolvePath("/foo")} will return {@literal /foo}</li>
     * </ul>
     * <p>
     * Given {@literal quarkus.http.root-path=/} and
     * {@literal quarkus.http.non-application-root-path="/q"}
     * <ul>
     * <li>{@code resolvePath("foo")} will return {@literal /q/foo}</li>
     * <li>{@code resolvePath("/foo")} will return {@literal /foo}</li>
     * </ul>
     * Given {@literal quarkus.http.root-path=/app} and
     * {@literal quarkus.http.non-application-root-path="q"}
     * <ul>
     * <li>{@code resolvePath("foo")} will return {@literal /app/q/foo}</li>
     * <li>{@code resolvePath("/foo")} will return {@literal /foo}</li>
     * </ul>
     * Given {@literal quarkus.http.root-path=/app} and
     * {@literal quarkus.http.non-application-root-path="/q"}
     * <ul>
     * <li>{@code resolvePath("foo")} will return {@literal /q/foo}</li>
     * <li>{@code resolvePath("/foo")} will return {@literal /foo}</li>
     * </ul>
     * <p>
     * The returned path will not end with a slash.
     *
     * @param path Path to be resolved to an absolute path.
     * @return An absolute path not ending with a slash
     * @see UriNormalizationUtil#normalizeWithBase(URI, String, boolean)
     * @throws IllegalArgumentException if path is null or empty
     */
    public String resolvePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Specified path can not be empty");
        }
        return UriNormalizationUtil.normalizeWithBase(nonApplicationRootPath, path, false).getPath();
    }

    /**
     * Resolve a base path and a sub-resource against the non-application root.
     * This will call resolvePath on the base path (to establish a fully-resolved,
     * absolute path), and then will resolve the subRoute against that resolved path.
     * This allows a configured subpath to be configured (consistently)
     * as an absolute URI.
     * 
     * Given {@literal quarkus.http.root-path=/} and
     * {@literal quarkus.http.non-application-root-path="q"}
     * <ul>
     * <li>{@code resolveNestedPath("foo", "a")} will return {@literal /q/foo/a}</li>
     * <li>{@code resolveNestedPath("foo", "/a)} will return {@literal /a}</li>
     * </ul>
     * <p>
     * The returned path will not end with a slash.
     *
     * @param path Path to be resolved to an absolute path.
     * @return An absolute path not ending with a slash
     * @see UriNormalizationUtil#normalizeWithBase(URI, String, boolean)
     * @see #resolvePath(String)
     * @throws IllegalArgumentException if path is null or empty
     */
    public String resolveNestedPath(String path, String subRoute) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Specified path can not be empty");
        }
        URI base = UriNormalizationUtil.normalizeWithBase(nonApplicationRootPath, path, true);
        return UriNormalizationUtil.normalizeWithBase(base, subRoute, false).getPath();
    }

    public Builder routeBuilder() {
        return new Builder(this);
    }

    /**
     * Per non-application endpoint instance.
     */
    public static class Builder extends RouteBuildItem.Builder {
        private final NonApplicationRootPathBuildItem buildItem;
        private RouteBuildItem.RouteType routeType = RouteBuildItem.RouteType.FRAMEWORK_ROUTE;
        private String path;

        Builder(NonApplicationRootPathBuildItem buildItem) {
            this.buildItem = buildItem;
        }

        @Override
        public Builder routeFunction(Function<Router, Route> routeFunction) {
            throw new RuntimeException(
                    "This method is not supported using this builder. Use #routeFunction(String, Consumer<Route>)");
        }

        public Builder routeFunction(String route, Consumer<Route> routeFunction) {
            route = super.absolutePath = buildItem.resolvePath(route);

            boolean isFrameworkRoute = buildItem.dedicatedRouterRequired
                    && route.startsWith(buildItem.getNonApplicationRootPath());

            if (isFrameworkRoute) {
                // relative non-application root (leading slash for vert.x)
                this.path = "/" + UriNormalizationUtil.relativize(buildItem.getNonApplicationRootPath(), route);
                this.routeType = RouteBuildItem.RouteType.FRAMEWORK_ROUTE;
            } else if (route.startsWith(buildItem.httpRootPath.getPath())) {
                // relative to http root (leading slash for vert.x route)
                this.path = "/" + UriNormalizationUtil.relativize(buildItem.httpRootPath.getPath(), route);
                this.routeType = RouteBuildItem.RouteType.APPLICATION_ROUTE;
            } else if (route.startsWith("/")) {
                // absolute path
                this.path = route;
                this.routeType = RouteBuildItem.RouteType.ABSOLUTE_ROUTE;
            }

            super.routeFunction(this.path, routeFunction);
            return this;
        }

        @Override
        public Builder route(String route) {
            routeFunction(route, null);
            return this;
        }

        public Builder nestedRoute(String baseRoute, String subRoute) {
            if (subRoute.startsWith("/")) {
                routeFunction(subRoute, null);
                return this;
            }

            baseRoute = baseRoute.endsWith("/") ? baseRoute : baseRoute + "/";
            routeFunction(baseRoute + subRoute, null);
            return this;
        }

        @Override
        public Builder handler(Handler<RoutingContext> handler) {
            super.handler(handler);
            return this;
        }

        @Override
        public Builder handlerType(HandlerType handlerType) {
            super.handlerType(handlerType);
            return this;
        }

        @Override
        public Builder blockingRoute() {
            super.blockingRoute();
            return this;
        }

        @Override
        public Builder failureRoute() {
            super.failureRoute();
            return this;
        }

        @Override
        public Builder displayOnNotFoundPage() {
            super.displayOnNotFoundPage();
            return this;
        }

        @Override
        public Builder displayOnNotFoundPage(String notFoundPageTitle) {
            super.displayOnNotFoundPage(notFoundPageTitle);
            return this;
        }

        @Override
        public Builder routeConfigKey(String attributeName) {
            super.routeConfigKey(attributeName);
            return this;
        }

        @Override
        public RouteBuildItem build() {
            return new RouteBuildItem(this, routeType);
        }

        @Override
        protected ConfiguredPathInfo getRouteConfigInfo() {
            return super.getRouteConfigInfo();
        }

        @Override
        protected NotFoundPageDisplayableEndpointBuildItem getNotFoundEndpoint() {
            return super.getNotFoundEndpoint();
        }
    }
}
