package io.quarkus.vertx.http.deployment;

import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Function;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.util.UriNormalizationUtil;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.quarkus.vertx.http.deployment.devmode.console.ConfiguredPathInfo;
import io.quarkus.vertx.http.runtime.BasicRoute;
import io.quarkus.vertx.http.runtime.HandlerType;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public final class HttpRootPathBuildItem extends SimpleBuildItem {

    /**
     * Normalized from quarkus.http.root-path.
     * This path will always end in a slash
     */
    private final URI rootPath;

    public HttpRootPathBuildItem(String rootPath) {
        this.rootPath = UriNormalizationUtil.toURI(rootPath, true);
    }

    /**
     * Return normalized Http root path configured from {@literal quarkus.http.root-path}.
     * This path will always end in a slash.
     * <p>
     * Use {@link #resolvePath(String)} if you need to construct a Uri from the Http root path.
     *
     * @return Normalized Http root path ending with a slash
     * @see #resolvePath(String)
     */
    public String getRootPath() {
        return rootPath.getPath();
    }

    /**
     * Resolve path into an absolute path.
     * If path is relative, it will be resolved against `quarkus.http.root-path`.
     * An absolute path will be normalized and returned.
     * <p>
     * Given {@literal quarkus.http.root-path=/}
     * <ul>
     * <li>{@code resolvePath("foo")} will return {@literal /foo}</li>
     * <li>{@code resolvePath("/foo")} will return {@literal /foo}</li>
     * </ul>
     * Given {@literal quarkus.http.root-path=/app}
     * <ul>
     * <li>{@code resolvePath("foo")} will return {@literal /app/foo}</li>
     * <li>{@code resolvePath("/foo")} will return {@literal /foo}</li>
     * </ul>
     * <p>
     * The returned path will not end with a slash.
     *
     * @param path Path to be resolved to an absolute path.
     * @return An absolute path not ending with a slash
     * @see UriNormalizationUtil#normalizeWithBase(URI, String, boolean)
     */
    public String resolvePath(String path) {
        return UriNormalizationUtil.normalizeWithBase(rootPath, path, false).getPath();
    }

    /**
     * Resolve path that is always relative into an absolute path.
     * Whether the path is relative or absolute, it will be resolved against `quarkus.http.root-path`,
     * by removing the '/' in the latter case.
     * <p>
     * Given {@literal quarkus.http.root-path=/}
     * <ul>
     * <li>{@code relativePath("foo")} will return {@literal /foo}</li>
     * <li>{@code relativePath("/foo")} will return {@literal /foo}</li>
     * </ul>
     * Given {@literal quarkus.http.root-path=/app}
     * <ul>
     * <li>{@code relativePath("foo")} will return {@literal /app/foo}</li>
     * <li>{@code relativePath("/foo")} will return {@literal /app/foo}</li>
     * </ul>
     * <p>
     * The returned path will not end with a slash.
     *
     * @param path Path to be resolved to an absolute path.
     * @return An absolute path not ending with a slash
     * @see UriNormalizationUtil#normalizeWithBase(URI, String, boolean)
     */
    public String relativePath(String path) {
        String relativePath = path.startsWith("/") ? path.substring(1) : path;
        return UriNormalizationUtil.normalizeWithBase(rootPath, relativePath, false).getPath();
    }

    public HttpRootPathBuildItem.Builder routeBuilder() {
        return new HttpRootPathBuildItem.Builder(this);
    }

    public static class Builder extends RouteBuildItem.Builder {
        private final HttpRootPathBuildItem buildItem;
        private RouteBuildItem.RouteType routeType = RouteBuildItem.RouteType.APPLICATION_ROUTE;
        private String path;

        Builder(HttpRootPathBuildItem buildItem) {
            this.buildItem = buildItem;
        }

        @Override
        public Builder routeFunction(Function<Router, Route> routeFunction) {
            throw new RuntimeException(
                    "This method is not supported using this builder. Use #routeFunction(String, Consumer<Route>)");
        }

        public Builder orderedRoute(String route, Integer order) {
            route = super.absolutePath = buildItem.resolvePath(route);

            if (route.startsWith(buildItem.getRootPath())) {
                // relative to http root (leading slash for vert.x route)
                this.path = "/" + UriNormalizationUtil.relativize(buildItem.getRootPath(), route);
                this.routeType = RouteBuildItem.RouteType.APPLICATION_ROUTE;
            } else if (route.startsWith("/")) {
                // absolute path
                this.path = route;
                this.routeType = RouteBuildItem.RouteType.ABSOLUTE_ROUTE;
            }

            BasicRoute basicRoute = new BasicRoute(this.path, -1);

            super.routeFunction = basicRoute;
            return this;
        }

        public Builder routeFunction(String route, Consumer<Route> routeFunction) {
            route = super.absolutePath = buildItem.resolvePath(route);

            if (route.startsWith(buildItem.getRootPath())) {
                // relative to http root (leading slash for vert.x route)
                this.path = "/" + UriNormalizationUtil.relativize(buildItem.getRootPath(), route);
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
