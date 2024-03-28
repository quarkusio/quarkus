package io.quarkus.keycloak.pep.runtime;

import static io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerUtil.createPolicyEnforcer;
import static io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerUtil.getOidcTenantConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import org.keycloak.adapters.authorization.PolicyEnforcer;

import io.quarkus.keycloak.pep.PolicyEnforcerResolver;
import io.quarkus.keycloak.pep.TenantPolicyConfigResolver;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class DefaultPolicyEnforcerResolver implements PolicyEnforcerResolver {

    private final TenantPolicyConfigResolver dynamicConfigResolver;
    private final TenantPolicyConfigResolver.KeycloakRequestContext requestContext;
    private final Map<String, PolicyEnforcer> namedPolicyEnforcers;
    private final PolicyEnforcer defaultPolicyEnforcer;
    private final long readTimeout;
    private final boolean tlsConfigTrustAll;
    private final OidcConfig oidcConfig;

    DefaultPolicyEnforcerResolver(OidcConfig oidcConfig, KeycloakPolicyEnforcerConfig config, TlsConfig tlsConfig,
            HttpConfiguration httpConfiguration, BlockingSecurityExecutor blockingSecurityExecutor,
            Instance<TenantPolicyConfigResolver> configResolver) {
        this.readTimeout = httpConfiguration.readTimeout.toMillis();
        this.oidcConfig = oidcConfig;
        this.tlsConfigTrustAll = tlsConfig.trustAll;
        this.defaultPolicyEnforcer = createPolicyEnforcer(oidcConfig.defaultTenant, config.defaultTenant(), tlsConfigTrustAll);
        this.namedPolicyEnforcers = createNamedPolicyEnforcers(oidcConfig, config, tlsConfigTrustAll);
        if (configResolver.isResolvable()) {
            this.dynamicConfigResolver = configResolver.get();
            this.requestContext = createKeycloakRequestContext(blockingSecurityExecutor);
        } else {
            this.dynamicConfigResolver = null;
            this.requestContext = null;
        }
    }

    @Override
    public Uni<PolicyEnforcer> resolvePolicyEnforcer(RoutingContext routingContext, String tenantId) {
        if (dynamicConfigResolver == null) {
            return Uni.createFrom().item(getStaticPolicyEnforcer(tenantId));
        } else {
            return getDynamicPolicyEnforcer(routingContext, tenantId)
                    .onItem().ifNull().continueWith(new Supplier<PolicyEnforcer>() {
                        @Override
                        public PolicyEnforcer get() {
                            return getStaticPolicyEnforcer(tenantId);
                        }
                    });
        }
    }

    @Override
    public long getReadTimeout() {
        return readTimeout;
    }

    PolicyEnforcer getStaticPolicyEnforcer(String tenantId) {
        return tenantId != null && namedPolicyEnforcers.containsKey(tenantId)
                ? namedPolicyEnforcers.get(tenantId)
                : defaultPolicyEnforcer;
    }

    boolean hasDynamicPolicyEnforcers() {
        return dynamicConfigResolver != null;
    }

    private Uni<PolicyEnforcer> getDynamicPolicyEnforcer(RoutingContext routingContext, String tenantId) {
        return dynamicConfigResolver.resolve(routingContext, tenantId, requestContext)
                .onItem().ifNotNull().transform(new Function<KeycloakPolicyEnforcerTenantConfig, PolicyEnforcer>() {
                    @Override
                    public PolicyEnforcer apply(KeycloakPolicyEnforcerTenantConfig tenant) {
                        return createPolicyEnforcer(tenant, tlsConfigTrustAll, tenantId, oidcConfig);
                    }
                });
    }

    private static Map<String, PolicyEnforcer> createNamedPolicyEnforcers(OidcConfig oidcConfig,
            KeycloakPolicyEnforcerConfig config, boolean tlsConfigTrustAll) {
        if (config.namedTenants().isEmpty()) {
            return Map.of();
        }

        Map<String, PolicyEnforcer> policyEnforcerTenants = new HashMap<>();
        for (Map.Entry<String, KeycloakPolicyEnforcerTenantConfig> tenant : config.namedTenants().entrySet()) {
            OidcTenantConfig oidcTenantConfig = getOidcTenantConfig(oidcConfig, tenant.getKey());
            policyEnforcerTenants.put(tenant.getKey(),
                    createPolicyEnforcer(oidcTenantConfig, tenant.getValue(), tlsConfigTrustAll));
        }
        return Map.copyOf(policyEnforcerTenants);
    }

    private static TenantPolicyConfigResolver.KeycloakRequestContext createKeycloakRequestContext(
            BlockingSecurityExecutor blockingSecurityExecutor) {
        return new TenantPolicyConfigResolver.KeycloakRequestContext() {
            @Override
            public <T> Uni<T> runBlocking(Supplier<T> function) {
                return blockingSecurityExecutor.executeBlocking(function);
            }
        };
    }
}
