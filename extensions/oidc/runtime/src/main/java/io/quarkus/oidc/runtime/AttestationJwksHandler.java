package io.quarkus.oidc.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.quarkus.vertx.http.runtime.security.ImmutablePathMatcher;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class AttestationJwksHandler implements Handler<RoutingContext> {

    private static final Logger LOG = Logger.getLogger(AttestationJwksHandler.class);
    public static final String ATTESTATION_JWKS_WELL_KNOWN_PATH = "/.well-known/attestation-jwks";

    record NewAttestationJwks() {
    }

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
            Handler<RoutingContext> routeHandler = matcher
                    .match(HttpSecurityUtils.pathWithoutMatrixParams(ctx.normalizedPath())).getValue();
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

    synchronized void updatePathMatcher(@Observes NewAttestationJwks ignored) {
        createOrUpdatePathMatcher();
    }

    private void createOrUpdatePathMatcher() {
        List<OidcTenantConfig> attestationConfigs = new ArrayList<>();
        for (TenantConfigContext configContext : resolver.getTenantConfigBean().getAllTenantConfigs()) {
            if (configContext.ready() && configContext.oidcConfig().tenantEnabled()
                    && configContext.oidcConfig().credentials().attestation().enabled()) {
                attestationConfigs.add(configContext.oidcConfig());
            }
        }
        pathMatcher = buildPathMatcher(attestationConfigs, registry, resolver.getRootPath());
    }

    static ImmutablePathMatcher<Handler<RoutingContext>> buildPathMatcher(List<OidcTenantConfig> attestationConfigs,
            AttestationKeyRegistry registry, String rootPath) {
        ImmutablePathMatcher.ImmutablePathMatcherBuilder<Handler<RoutingContext>> builder = null;
        Map<String, OidcTenantConfig> pathCache = null;
        for (OidcTenantConfig oidcConfig : attestationConfigs) {
            String tenantId = oidcConfig.tenantId().get();
            String clientId = oidcConfig.clientId().get();
            // Only register a JWKS route for self-attesting clients: attesters that delegate to a remote
            // attestation service have no attestation key pair and so no public key to publish.
            if (registry.getAttestationSigningKey(tenantId) == null) {
                continue;
            }
            if (builder == null) {
                builder = ImmutablePathMatcher.builder();
                pathCache = new HashMap<>();
            }
            String routePath = getAttestationJwksPath(oidcConfig, rootPath);
            OidcTenantConfig previousConfig = pathCache.put(routePath, oidcConfig);
            if (previousConfig == null) {
                builder.addPath(routePath, new RouteHandler(tenantId, clientId, registry));
            } else {
                String previousTenantId = previousConfig.tenantId().get();
                String currentTenantId = oidcConfig.tenantId().get();
                if (!previousTenantId.equals(currentTenantId)) {
                    String errorMessage = "OIDC tenants '%s' and '%s' share the same attestation JWKS path '%s', which is not supported"
                            .formatted(previousTenantId, currentTenantId, routePath);
                    LOG.error(errorMessage);
                    throw new OIDCException(errorMessage);
                }
            }
        }
        return builder != null ? builder.build() : null;
    }

    static String getAttestationJwksPath(OidcTenantConfig oidcConfig, String configuredRootPath) {
        String path = OidcUtils.getRootPath(configuredRootPath) + ATTESTATION_JWKS_WELL_KNOWN_PATH;
        if (!OidcUtils.DEFAULT_TENANT_ID.equals(oidcConfig.tenantId().get())) {
            path += OidcCommonUtils.prependSlash(oidcConfig.tenantId().get().toLowerCase());
        }
        return path;
    }

    static void fireAttestationJwksChangedEvent(OidcTenantConfig oidcConfig, TenantConfigContext tenant) {
        if (oidcConfig.credentials().attestation().enabled() ||
                (tenant.oidcConfig() != null && tenant.oidcConfig().credentials().attestation().enabled())) {
            boolean attestationChanged = tenant.oidcConfig() == null
                    || oidcConfig.credentials().attestation().enabled() != tenant.oidcConfig().credentials().attestation()
                            .enabled();
            if (attestationChanged) {
                fireAttestationJwksEvent();
            }
        }
    }

    static void fireAttestationJwksReadyEvent(OidcTenantConfig oidcConfig) {
        if (oidcConfig.credentials().attestation().enabled()) {
            fireAttestationJwksEvent();
        }
    }

    private static void fireAttestationJwksEvent() {
        Event<NewAttestationJwks> event = Arc.container().beanManager().getEvent()
                .select(NewAttestationJwks.class);
        event.fire(new NewAttestationJwks());
    }

    private static class RouteHandler implements Handler<RoutingContext> {
        private final String tenantId;
        private final String clientId;
        private final AttestationKeyRegistry registry;

        RouteHandler(String tenantId, String clientId, AttestationKeyRegistry registry) {
            this.tenantId = tenantId;
            this.clientId = clientId;
            this.registry = registry;
        }

        @Override
        public void handle(RoutingContext ctx) {
            LOG.debugf("Attestation JWKS request for client %s received", clientId);
            ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .putHeader("Cache-Control", "no-store")
                    .end(registry.getJwkSet(tenantId));
        }
    }
}
