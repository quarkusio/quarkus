package io.quarkus.oidc.runtime;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.runtime.BackChannelLogoutHandler.NewBackChannelLogoutPath;
import io.quarkus.vertx.http.runtime.security.ImmutablePathMatcher;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class ResourceMetadataHandler implements Handler<RoutingContext> {
    private static final Logger LOG = Logger.getLogger(ResourceMetadataHandler.class);
    private static final String SLASH = "/";
    private static final String HTTP_SCHEME = "http";
    private final DefaultTenantConfigResolver resolver;
    private volatile ImmutablePathMatcher<Handler<RoutingContext>> pathMatcher;

    record NewResourceMetadataPath() {
    }

    ResourceMetadataHandler(DefaultTenantConfigResolver resolver) {
        this.resolver = resolver;
        this.pathMatcher = null;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        var matcher = pathMatcher;
        if (matcher != null) {
            Handler<RoutingContext> routeHandler = matcher.match(routingContext.normalizedPath()).getValue();
            if (routeHandler != null) {
                routeHandler.handle(routingContext);
                return;
            }
        }

        routingContext.next();
    }

    void setup(@Observes Router router) {
        createOrUpdatePathMatcher();
    }

    synchronized void updatePathMatcher(@Observes NewBackChannelLogoutPath ignored) {
        createOrUpdatePathMatcher();
    }

    private void createOrUpdatePathMatcher() {
        ImmutablePathMatcher.ImmutablePathMatcherBuilder<Handler<RoutingContext>> builder = null;
        Map<String, OidcTenantConfig> pathCache = null;
        for (TenantConfigContext configContext : resolver.getTenantConfigBean().getAllTenantConfigs()) {
            if (configContext.ready() && configContext.oidcConfig().tenantEnabled()
                    && configContext.oidcConfig().resourceMetadata().enabled()) {
                if (builder == null) {
                    builder = ImmutablePathMatcher.builder();
                    pathCache = new HashMap<>();
                }
                String routePath = getResourceMetadataPath(configContext.oidcConfig());
                if (routePath.contains("*")) {
                    throw new IllegalStateException("Resource metadata path cannot contain a wildcard '*' character");
                }
                OidcTenantConfig previousConfig = pathCache.put(routePath, configContext.oidcConfig());
                if (previousConfig == null) {
                    Handler<RoutingContext> routeHandler = new RouteHandler(configContext.oidcConfig(), resolver);
                    builder.addPath(routePath, routeHandler);
                } else {
                    String previousTenantId = previousConfig.tenantId().get();
                    String currentTenantId = configContext.oidcConfig().tenantId().get();
                    // maybe invalid state, but technically it could happen that some produces a static tenant with
                    // a same id as a dynamic tenant
                    if (!previousTenantId.equals(currentTenantId)) {
                        String errorMessage = "OIDC tenants '%s' and '%s' share the same resource metadata path '%s', which is not supported"
                                .formatted(previousTenantId, currentTenantId, routePath);
                        LOG.error(errorMessage);
                        throw new OIDCException(errorMessage);
                    }
                }
            }
        }
        if (builder != null) {
            pathMatcher = builder.build();
        } else {
            pathMatcher = null;
        }
    }

    private String getResourceMetadataPath(OidcTenantConfig oidcConfig) {
        String configuredResource = oidcConfig.resourceMetadata().resource().orElse("");

        String relativePath = null;

        if (configuredResource.startsWith(HTTP_SCHEME)) {
            relativePath = URI.create(configuredResource).getRawPath();
        } else {
            relativePath = configuredResource;
        }

        String protectedResourceMetadataPath = getRootPath() + OidcConstants.RESOURCE_METADATA_WELL_KNOWN_PATH;
        if (!relativePath.isEmpty()) {
            if (!SLASH.equals(relativePath)) {
                protectedResourceMetadataPath += OidcCommonUtils.prependSlash(relativePath);
            }
        } else if (!OidcUtils.DEFAULT_TENANT_ID.equals(oidcConfig.tenantId().get())) {
            protectedResourceMetadataPath += OidcCommonUtils.prependSlash(oidcConfig.tenantId().get().toLowerCase());
        }

        return protectedResourceMetadataPath;
    }

    private String getRootPath() {
        // Prepend '/' if it is not present
        String rootPath = OidcCommonUtils.prependSlash(resolver.getRootPath());
        // Strip trailing '/' if the length is > 1
        if (rootPath.length() > 1 && rootPath.endsWith("/")) {
            rootPath = rootPath.substring(rootPath.length() - 1);
        }
        // if it is only '/' then return an empty value
        return SLASH.equals(rootPath) ? "" : rootPath;
    }

    private static class RouteHandler implements Handler<RoutingContext> {
        private final OidcTenantConfig oidcConfig;
        private final DefaultTenantConfigResolver resolver;

        RouteHandler(OidcTenantConfig oidcTenantConfig, DefaultTenantConfigResolver resolver) {
            this.oidcConfig = oidcTenantConfig;
            this.resolver = resolver;
        }

        @Override
        public void handle(RoutingContext context) {
            LOG.debugf("Resource metadata request for the tenant %s received", oidcConfig.tenantId().get());
            context.response().setStatusCode(200);
            context.response().end(prepareMetadata(context));
        }

        private String prepareMetadata(RoutingContext context) {
            JsonObject metadata = new JsonObject();

            String configuredResource = oidcConfig.resourceMetadata().resource().orElse("");

            if (configuredResource.startsWith(HTTP_SCHEME)) {
                metadata.put(OidcConstants.RESOURCE_METADATA_RESOURCE, configuredResource);
            } else {
                if (!configuredResource.isEmpty()) {
                    if (!SLASH.equals(configuredResource)) {
                        configuredResource = OidcCommonUtils.prependSlash(configuredResource);
                    }
                } else if (!OidcUtils.DEFAULT_TENANT_ID.equals(oidcConfig.tenantId().get())) {
                    configuredResource += OidcCommonUtils.prependSlash(oidcConfig.tenantId().get().toLowerCase());
                }
                String authority = URI.create(context.request().absoluteURI()).getAuthority();
                String resourceIdentifier = buildUri(context,
                        oidcConfig.resourceMetadata().forceHttpsScheme(), authority, configuredResource);
                metadata.put(OidcConstants.RESOURCE_METADATA_RESOURCE, resourceIdentifier);
            }
            JsonArray authorizationServers = new JsonArray();
            authorizationServers.add(0, oidcConfig.authServerUrl().get());
            metadata.put(OidcConstants.RESOURCE_METADATA_AUTHORIZATION_SERVERS, authorizationServers);
            return metadata.toString();
        }

        private String buildUri(RoutingContext context, boolean forceHttps, String authority, String path) {
            final String scheme = forceHttps ? "https" : context.request().scheme();
            String forwardedPrefix = "";
            if (resolver.isEnableHttpForwardedPrefix()) {
                String forwardedPrefixHeader = context.request().getHeader("X-Forwarded-Prefix");
                if (forwardedPrefixHeader != null && !forwardedPrefixHeader.equals("/")
                        && !forwardedPrefixHeader.equals("//")) {
                    forwardedPrefix = forwardedPrefixHeader;
                    if (forwardedPrefix.endsWith("/")) {
                        forwardedPrefix = forwardedPrefix.substring(0, forwardedPrefix.length() - 1);
                    }
                }
            }
            return new StringBuilder(scheme).append("://")
                    .append(authority)
                    .append(forwardedPrefix)
                    .append(path)
                    .toString();
        }
    }
}
