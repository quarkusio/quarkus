package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.runtime.OptionalOidcRouteHandler.OptionalOidcRouteHandlerBuilder.isRouteRequired;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.vertx.http.runtime.security.ImmutablePathMatcher;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class ResourceMetadataHandler implements OptionalOidcRouteHandler {
    private static final Logger LOG = Logger.getLogger(ResourceMetadataHandler.class);
    private static final String SLASH = "/";
    private static final String HTTP_SCHEME = "http";
    private static final String RESOURCE_METADATA_AUTHENTICATE_PARAM = "resource_metadata";
    private final DefaultTenantConfigResolver resolver;
    private volatile ImmutablePathMatcher<Handler<RoutingContext>> pathMatcher;

    private ResourceMetadataHandler() {
        this.resolver = Arc.requireContainer().select(DefaultTenantConfigResolver.class).get();
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
                String routePath = getResourceMetadataPath(configContext.oidcConfig(), resolver.getRootPath());
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

    static String getResourceMetadataPath(OidcTenantConfig oidcConfig, String configuredRootPath) {
        String configuredResource = oidcConfig.resourceMetadata().resource().orElse("");

        String relativePath = null;

        if (configuredResource.startsWith(HTTP_SCHEME)) {
            relativePath = URI.create(configuredResource).getRawPath();
        } else {
            relativePath = configuredResource;
        }

        String protectedResourceMetadataPath = OidcUtils.getRootPath(configuredRootPath)
                + OidcConstants.RESOURCE_METADATA_WELL_KNOWN_PATH;
        if (!relativePath.isEmpty()) {
            if (!SLASH.equals(relativePath)) {
                protectedResourceMetadataPath += OidcCommonUtils.prependSlash(relativePath);
            }
        } else if (!OidcUtils.DEFAULT_TENANT_ID.equals(oidcConfig.tenantId().get())) {
            protectedResourceMetadataPath += OidcCommonUtils.prependSlash(oidcConfig.tenantId().get().toLowerCase());
        }

        return protectedResourceMetadataPath;
    }

    @Override
    public void updateIfPathChanged(OidcTenantConfig oidcConfig, TenantConfigContext tenant,
            TenantConfigBean tenantConfigBean) {
        if (tenant == null) {
            if (oidcConfig.resourceMetadata().enabled()) {
                createOrUpdatePathMatcher();
            }
        } else if (oidcConfig.resourceMetadata().enabled() ||
                (tenant.oidcConfig() != null && tenant.oidcConfig().resourceMetadata().enabled())) {
            boolean resourceChanged = tenant.oidcConfig() == null
                    || !oidcConfig.resourceMetadata().resource().orElse("")
                            .equals(tenant.oidcConfig().resourceMetadata().resource().orElse(""))
                    || oidcConfig.resourceMetadata().enabled() != tenant.oidcConfig().resourceMetadata().enabled()
                    || oidcConfig.resourceMetadata().forceHttpsScheme() != tenant.oidcConfig().resourceMetadata()
                            .forceHttpsScheme();
            if (resourceChanged) {
                createOrUpdatePathMatcher();
            }
        }
    }

    @Override
    public void initialize() {
        createOrUpdatePathMatcher();
    }

    @Override
    public void close() {
        // nothing to do
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

            String resourceIdentifier = buildResourceIdentifierUrl(context, resolver, oidcConfig);
            metadata.put(OidcConstants.RESOURCE_METADATA_RESOURCE, resourceIdentifier);

            if (oidcConfig.resourceMetadata().scopes().isPresent()) {
                JsonArray scopes = new JsonArray();
                for (String scope : oidcConfig.resourceMetadata().scopes().get()) {
                    scopes.add(scope);
                }
                metadata.put(OidcConstants.RESOURCE_METADATA_SCOPES, scopes);
            }

            JsonArray authorizationServers = new JsonArray();
            authorizationServers.add(0, oidcConfig.resourceMetadata().authorizationServer()
                    .orElse(oidcConfig.authServerUrl().get()));
            metadata.put(OidcConstants.RESOURCE_METADATA_AUTHORIZATION_SERVERS, authorizationServers);
            return metadata.toString();
        }

    }

    static String resourceMetadataAuthenticateParameter(RoutingContext context, DefaultTenantConfigResolver resolver,
            OidcTenantConfig oidcConfig) {
        return " " + RESOURCE_METADATA_AUTHENTICATE_PARAM + "=\""
                + buildAbsoluteResourceIdentifierUrl(context, resolver, oidcConfig)
                + "\"";
    }

    static String buildResourceIdentifierUrl(RoutingContext context, DefaultTenantConfigResolver resolver,
            OidcTenantConfig oidcConfig) {
        String configuredResource = oidcConfig.resourceMetadata().resource().orElse("");

        if (configuredResource.startsWith(HTTP_SCHEME)) {
            return configuredResource;
        } else {
            if (!configuredResource.isEmpty()) {
                if (!SLASH.equals(configuredResource)) {
                    configuredResource = OidcCommonUtils.prependSlash(configuredResource);
                }
            } else if (!OidcUtils.DEFAULT_TENANT_ID.equals(oidcConfig.tenantId().get())) {
                configuredResource += OidcCommonUtils.prependSlash(oidcConfig.tenantId().get().toLowerCase());
            }
            String authority = URI.create(context.request().absoluteURI()).getAuthority();
            return buildUri(context, resolver.isEnableHttpForwardedPrefix(),
                    oidcConfig.resourceMetadata().forceHttpsScheme(), authority, configuredResource);
        }
    }

    public static String buildAbsoluteResourceIdentifierUrl(RoutingContext context, DefaultTenantConfigResolver resolver,
            OidcTenantConfig oidcConfig) {
        return buildAbsoluteResourceIdentifierUrl(context, oidcConfig,
                resolver.getRootPath(), resolver.isEnableHttpForwardedPrefix());
    }

    public static String buildAbsoluteResourceIdentifierUrl(RoutingContext context, OidcTenantConfig oidcConfig,
            String httpRootPath, boolean enableHttpForwardedPrefix) {
        String configuredResource = getResourceMetadataPath(oidcConfig, httpRootPath);

        if (configuredResource.startsWith(HTTP_SCHEME)) {
            return configuredResource;
        } else {
            String authority = URI.create(context.request().absoluteURI()).getAuthority();
            return buildUri(context, enableHttpForwardedPrefix,
                    oidcConfig.resourceMetadata().forceHttpsScheme(), authority, configuredResource);
        }
    }

    private static String buildUri(RoutingContext context,
            boolean enableHttpForwardedPrefix, boolean forceHttps, String authority, String path) {
        final String scheme = forceHttps ? "https" : context.request().scheme();
        String forwardedPrefix = "";
        if (enableHttpForwardedPrefix) {
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

    public static final class ResourceMetadataHandlerBuilder implements OptionalOidcRouteHandlerBuilder {

        @Override
        public OptionalOidcRouteHandler build(Collection<OidcTenantConfig> staticTenantConfigs, Vertx ignored) {
            if (isRouteRequired(staticTenantConfigs, tenantConfig -> tenantConfig.resourceMetadata().enabled())) {
                return new ResourceMetadataHandler();
            }
            return null;
        }
    }
}
