package io.quarkus.keycloak.pep.runtime;

import static io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerUtil.createPolicyEnforcer;
import static io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerUtil.getOidcTenantConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

import org.keycloak.adapters.authorization.PolicyEnforcer;

import io.quarkus.arc.InjectableInstance;
import io.quarkus.keycloak.pep.PolicyEnforcerResolver;
import io.quarkus.keycloak.pep.TenantPolicyConfigResolver;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.common.runtime.OidcTlsSupport;
import io.quarkus.oidc.runtime.BlockingTaskRunner;
import io.quarkus.oidc.runtime.TenantConfigBean;
import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class DefaultPolicyEnforcerResolver implements PolicyEnforcerResolver {

    private final TenantPolicyConfigResolver dynamicConfigResolver;
    private final BlockingTaskRunner<KeycloakPolicyEnforcerTenantConfig> requestContext;
    private final Map<String, PolicyEnforcer> namedPolicyEnforcers;
    private final PolicyEnforcer defaultPolicyEnforcer;
    private final long readTimeout;
    private final OidcTlsSupport tlsSupport;

    DefaultPolicyEnforcerResolver(TenantConfigBean tenantConfigBean, KeycloakPolicyEnforcerConfig config,
            VertxHttpConfig httpConfig, BlockingSecurityExecutor blockingSecurityExecutor,
            Instance<TenantPolicyConfigResolver> configResolver,
            InjectableInstance<TlsConfigurationRegistry> tlsConfigRegistryInstance) {
        this.readTimeout = httpConfig.readTimeout().toMillis();

        if (tlsConfigRegistryInstance.isResolvable()) {
            this.tlsSupport = OidcTlsSupport.of(tlsConfigRegistryInstance.get());
        } else {
            this.tlsSupport = OidcTlsSupport.empty();
        }

        var defaultTenantConfig = tenantConfigBean.getDefaultTenant().oidcConfig();
        var defaultTenantTlsSupport = tlsSupport.forConfig(defaultTenantConfig.tls());
        this.defaultPolicyEnforcer = createPolicyEnforcer(defaultTenantConfig, config.defaultTenant(),
                defaultTenantTlsSupport);
        this.namedPolicyEnforcers = createNamedPolicyEnforcers(tenantConfigBean, config, tlsSupport);
        if (configResolver.isResolvable()) {
            this.dynamicConfigResolver = configResolver.get();
            this.requestContext = new BlockingTaskRunner<>(blockingSecurityExecutor);
        } else {
            this.dynamicConfigResolver = null;
            this.requestContext = null;
        }
    }

    @Override
    public Uni<PolicyEnforcer> resolvePolicyEnforcer(RoutingContext routingContext, OidcTenantConfig tenantConfig) {
        if (tenantConfig == null) {
            return Uni.createFrom().item(defaultPolicyEnforcer);
        }
        if (dynamicConfigResolver == null) {
            return Uni.createFrom().item(getStaticPolicyEnforcer(tenantConfig.tenantId().get()));
        } else {
            return getDynamicPolicyEnforcer(routingContext, tenantConfig)
                    .onItem().ifNull().continueWith(new Supplier<PolicyEnforcer>() {
                        @Override
                        public PolicyEnforcer get() {
                            return getStaticPolicyEnforcer(tenantConfig.tenantId().get());
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

    private Uni<PolicyEnforcer> getDynamicPolicyEnforcer(RoutingContext routingContext, OidcTenantConfig config) {
        return dynamicConfigResolver.resolve(routingContext, config, requestContext)
                .onItem().ifNotNull().transform(new Function<KeycloakPolicyEnforcerTenantConfig, PolicyEnforcer>() {
                    @Override
                    public PolicyEnforcer apply(KeycloakPolicyEnforcerTenantConfig tenant) {
                        return createPolicyEnforcer(config, tenant, tlsSupport.forConfig(config.tls()));
                    }
                });
    }

    private static Map<String, PolicyEnforcer> createNamedPolicyEnforcers(TenantConfigBean tenantConfigBean,
            KeycloakPolicyEnforcerConfig config, OidcTlsSupport tlsSupport) {
        if (config.namedTenants().isEmpty()) {
            return Map.of();
        }

        Map<String, PolicyEnforcer> policyEnforcerTenants = new HashMap<>();
        for (Map.Entry<String, KeycloakPolicyEnforcerTenantConfig> tenant : config.namedTenants().entrySet()) {
            var oidcTenantConfig = getOidcTenantConfig(tenantConfigBean, tenant.getKey());
            policyEnforcerTenants.put(tenant.getKey(),
                    createPolicyEnforcer(oidcTenantConfig, tenant.getValue(), tlsSupport.forConfig(oidcTenantConfig.tls())));
        }
        return Map.copyOf(policyEnforcerTenants);
    }

}
