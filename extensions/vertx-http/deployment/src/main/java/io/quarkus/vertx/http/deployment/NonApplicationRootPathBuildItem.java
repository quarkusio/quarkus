package io.quarkus.vertx.http.deployment;

import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.util.UriNormalizationUtil;
import io.quarkus.vertx.http.deployment.devmode.ConfiguredPathInfo;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.quarkus.vertx.http.runtime.HandlerType;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;
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
     * Normalized from quarkus.management.root-path
     */
    final URI managementRootPath;

    /**
     * Non-Application root path is distinct from HTTP root path.
     */
    final boolean dedicatedRouterRequired;

    final boolean attachedToMainRouter;

    public NonApplicationRootPathBuildItem(String httpRootPath, String nonApplicationRootPath, String managementRootPath) {
        // Presume value always starts with a slash and is normalized
        this.httpRootPath = UriNormalizationUtil.toURI(httpRootPath, true);

        this.nonApplicationRootPath = UriNormalizationUtil.normalizeWithBase(this.httpRootPath, nonApplicationRootPath,
                true);
        this.managementRootPath = managementRootPath == null ? null
                : UriNormalizationUtil.toURI("/" + managementRootPath, true);

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
     * @return the normalized root path for the mangement endpoints. {@code getNonApplicationRootPath()} if the
     *         management interface is disabled.
     */
    public String getManagementRootPath() {
        if (managementRootPath != null) {
            return managementRootPath.getPath();
        } else {
            return getNonApplicationRootPath();
        }
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
     * @throws IllegalArgumentException if path is null or empty
     * @see UriNormalizationUtil#normalizeWithBase(URI, String, boolean)
     */
    public String resolvePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Specified path can not be empty");
        }
        return UriNormalizationUtil.normalizeWithBase(nonApplicationRootPath, path, false).getPath();
    }

    public String resolveManagementPath(String path, ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig,
            LaunchModeBuildItem mode) {
        return resolveManagementPath(path, managementInterfaceBuildTimeConfig, mode, true);
    }

    public String resolveManagementPath(String path, ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig,
            LaunchModeBuildItem mode, boolean extensionOverride) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Specified path can not be empty");
        }
        if (managementInterfaceBuildTimeConfig.enabled && extensionOverride) {
            // Best effort
            String prefix = getManagementUrlPrefix(mode);
            if (managementRootPath != null) {
                return prefix + UriNormalizationUtil.normalizeWithBase(managementRootPath, path, false).getPath();
            } else {
                return prefix + path;
            }
        } else {
            if (managementRootPath != null) {
                return UriNormalizationUtil.normalizeWithBase(managementRootPath, path, false).getPath();
            }
            return UriNormalizationUtil.normalizeWithBase(nonApplicationRootPath, path, false).getPath();
        }
    }

    /**
     * Best effort to deduce the URL prefix (scheme, host, port) of the management interface.
     *
     * @param mode the mode, influencing the default port
     * @return the prefix
     */
    public static String getManagementUrlPrefix(LaunchModeBuildItem mode) {
        Config config = ConfigProvider.getConfig();
        var managementHost = config.getOptionalValue("quarkus.management.host", String.class).orElse("0.0.0.0");
        var managementPort = config.getOptionalValue("quarkus.management.port", Integer.class).orElse(9000);
        if (mode.isTest()) {
            managementPort = config.getOptionalValue("quarkus.management.test-port", Integer.class).orElse(9001);
        }
        var isHttps = isTLsConfigured(config);

        return (isHttps ? "https://" : "http://") + managementHost + ":" + managementPort;
    }

    /**
     * Resolve a base path and a sub-resource against the non-application root.
     * This will call resolvePath on the base path (to establish a fully-resolved,
     * absolute path), and then will resolve the subRoute against that resolved path.
     * This allows a configured subpath to be configured (consistently)
     * as an absolute URI.
     * <p>
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
     * @throws IllegalArgumentException if path is null or empty
     * @see UriNormalizationUtil#normalizeWithBase(URI, String, boolean)
     * @see #resolvePath(String)
     */
    public String resolveNestedPath(String path, String subRoute) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Specified path can not be empty");
        }
        URI base = UriNormalizationUtil.normalizeWithBase(nonApplicationRootPath, path, true);
        return UriNormalizationUtil.normalizeWithBase(base, subRoute, false).getPath();
    }

    public String resolveManagementNestedPath(String path, String subRoute) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Specified path can not be empty");
        }
        URI base;
        if (managementRootPath != null) {
            base = UriNormalizationUtil.normalizeWithBase(managementRootPath, path, true);
        } else {
            base = UriNormalizationUtil.normalizeWithBase(nonApplicationRootPath, path, true);
        }
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
        private RouteBuildItem.RouteType routerType = RouteBuildItem.RouteType.FRAMEWORK_ROUTE;
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
            return orderedRoute(route, null, routeFunction);
        }

        @Override
        public Builder route(String route) {
            routeFunction(route, null);
            return this;
        }

        @Override
        public Builder orderedRoute(String route, Integer order) {
            orderedRoute(route, order, null);
            return this;
        }

        @Override
        public Builder orderedRoute(String route, Integer order, Consumer<Route> routeFunction) {
            if (isManagement && this.buildItem.managementRootPath != null) {
                // The logic is slightly different when the management interface is enabled, as we have a single
                // router mounted at the root.
                if (route.startsWith("/")) {
                    this.path = route;
                } else {
                    this.path = buildItem.getManagementRootPath() + route;
                }
                this.routerType = RouteBuildItem.RouteType.ABSOLUTE_ROUTE;
                super.orderedRoute(this.path, order, routeFunction);
                return this;
            }

            route = super.absolutePath = buildItem.resolvePath(route);
            boolean isFrameworkRoute = buildItem.dedicatedRouterRequired
                    && route.startsWith(buildItem.getNonApplicationRootPath());

            if (isFrameworkRoute) {
                // relative non-application root (leading slash for vert.x)
                this.path = "/" + UriNormalizationUtil.relativize(buildItem.getNonApplicationRootPath(), route);
                this.routerType = RouteBuildItem.RouteType.FRAMEWORK_ROUTE;
            } else if (route.startsWith(buildItem.httpRootPath.getPath())) {
                // relative to http root (leading slash for vert.x route)
                this.path = "/" + UriNormalizationUtil.relativize(buildItem.httpRootPath.getPath(), route);
                this.routerType = RouteBuildItem.RouteType.APPLICATION_ROUTE;
            } else if (route.startsWith("/")) {
                // absolute path
                this.path = route;
                this.routerType = RouteBuildItem.RouteType.ABSOLUTE_ROUTE;
            }
            super.orderedRoute(this.path, order, routeFunction);
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
            return new RouteBuildItem(this, routeType, routerType, isManagement);
        }

        @Override
        protected ConfiguredPathInfo getRouteConfigInfo() {
            return super.getRouteConfigInfo();
        }

        @Override
        protected NotFoundPageDisplayableEndpointBuildItem getNotFoundEndpoint() {
            if (!displayOnNotFoundPage) {
                return null;
            }
            if (isManagement && buildItem.managementRootPath != null) {
                return null; // Exposed on the management interface, so not exposed.
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

        @Override
        public Builder management() {
            super.management();
            return this;
        }

        @Override
        public Builder management(String managementConfigKey) {
            super.management(managementConfigKey);
            return this;
        }
    }

    /**
     * Best effort to check if the management interface is using {@code https}.
     *
     * @param config the config
     * @return {@code true} if the management interface configuration contains a key or a certificate (indicating TLS)
     */
    private static boolean isTLsConfigured(Config config) {
        var hasCert = config.getOptionalValue("quarkus.management.ssl.certificate.file", String.class).isPresent();
        var hasKey = config.getOptionalValue("quarkus.management.ssl.certificate.key-file", String.class).isPresent();

        var hasKeys = config.getOptionalValue("quarkus.management.ssl.certificate.key-files", String.class).isPresent();
        var hasCerts = config.getOptionalValue("quarkus.management.ssl.certificate.files", String.class).isPresent();

        var hasProvider = config.getOptionalValue("quarkus.management.ssl.certificate.credential-provider", String.class)
                .isPresent();
        var hasProviderName = config
                .getOptionalValue("quarkus.management.ssl.certificate.credential-provider-name", String.class).isPresent();

        var hasKeyStore = config.getOptionalValue("quarkus.management.ssl.certificate.key-store-file", String.class)
                .isPresent();

        return hasCerts || hasKeys || hasCert || hasKey || hasProvider || hasProviderName || hasKeyStore;
    }
}
