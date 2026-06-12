package io.quarkus.oidc.runtime;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.common.runtime.AttestationKeyRegistry;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.vertx.http.runtime.security.ImmutablePathMatcher;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class AttestationJwksHandler implements Handler<RoutingContext> {

    private static final Logger LOG = Logger.getLogger(AttestationJwksHandler.class);
    public static final String ATTESTATION_JWKS_WELL_KNOWN_PATH = "/.well-known/attestation-jwks";

    private final DefaultTenantConfigResolver resolver;
    private final AttestationKeyRegistry registry;
    private volatile ImmutablePathMatcher<Handler<RoutingContext>> pathMatcher;

    AttestationJwksHandler(DefaultTenantConfigResolver resolver, AttestationKeyRegistry registry) {
        this.resolver = resolver;
        this.registry = registry;
        this.pathMatcher = null;
    }

    @Override
    public void handle(RoutingContext ctx) {
        var matcher = pathMatcher;
        if (matcher != null) {
            Handler<RoutingContext> routeHandler = matcher.match(ctx.normalizedPath()).getValue();
            if (routeHandler != null) {
                routeHandler.handle(ctx);
                return;
            }
        }
        ctx.next();
    }

    void setup(@Observes Router router) {
        createOrUpdatePathMatcher();
    }

    private void createOrUpdatePathMatcher() {
        ImmutablePathMatcher.ImmutablePathMatcherBuilder<Handler<RoutingContext>> builder = null;
        Map<String, OidcTenantConfig> pathCache = null;
        for (TenantConfigContext configContext : resolver.getTenantConfigBean().getAllTenantConfigs()) {
            if (configContext.ready() && configContext.oidcConfig().tenantEnabled()
                    && configContext.oidcConfig().credentials().attestation().enabled()) {
                if (builder == null) {
                    builder = ImmutablePathMatcher.builder();
                    pathCache = new HashMap<>();
                }
                String routePath = getAttestationJwksPath(configContext.oidcConfig(), resolver.getRootPath());
                OidcTenantConfig previousConfig = pathCache.put(routePath, configContext.oidcConfig());
                if (previousConfig == null) {
                    String clientId = configContext.oidcConfig().clientId().get();
                    builder.addPath(routePath, new RouteHandler(clientId, registry));
                } else {
                    String previousTenantId = previousConfig.tenantId().get();
                    String currentTenantId = configContext.oidcConfig().tenantId().get();
                    if (!previousTenantId.equals(currentTenantId)) {
                        String errorMessage = "OIDC tenants '%s' and '%s' share the same attestation JWKS path '%s', which is not supported"
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

    static String getAttestationJwksPath(OidcTenantConfig oidcConfig, String configuredRootPath) {
        String path = OidcUtils.getRootPath(configuredRootPath) + ATTESTATION_JWKS_WELL_KNOWN_PATH;
        if (!OidcUtils.DEFAULT_TENANT_ID.equals(oidcConfig.tenantId().get())) {
            path += OidcCommonUtils.prependSlash(oidcConfig.tenantId().get().toLowerCase());
        }
        return path;
    }

    private static class RouteHandler implements Handler<RoutingContext> {
        private final String clientId;
        private final AttestationKeyRegistry registry;

        RouteHandler(String clientId, AttestationKeyRegistry registry) {
            this.clientId = clientId;
            this.registry = registry;
        }

        @Override
        public void handle(RoutingContext ctx) {
            LOG.debugf("Attestation JWKS request for client %s received", clientId);
            ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .putHeader("Cache-Control", "no-store")
                    .end(registry.getJwkSet(clientId));
        }
    }
}
