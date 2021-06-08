package io.quarkus.keycloak.pep.runtime;

import java.util.Map;
import java.util.function.BiFunction;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.runtime.OidcConfig;
import io.smallrye.mutiny.Uni;

public class KeycloakPolicyEnforcerConfigBean {

    private final Map<String, KeycloakPolicyEnforcerContext> staticPolicyConfig;
    private final Map<String, KeycloakPolicyEnforcerContext> dynamicPolicyConfig;
    private final KeycloakPolicyEnforcerContext defaultPolicyConfig;
    private final OidcConfig staticOidcConfig;
    private final BiFunction<OidcTenantConfig, KeycloakPolicyEnforcerTenantConfig, Uni<KeycloakPolicyEnforcerContext>> policyConfigContextFactory;
    private final long readTimeout;

    public KeycloakPolicyEnforcerConfigBean(
            OidcConfig staticOidcConfig,
            KeycloakPolicyEnforcerContext defaultPolicyConfig,
            Map<String, KeycloakPolicyEnforcerContext> staticPoliciesConfig,
            Map<String, KeycloakPolicyEnforcerContext> dynamicPoliciesConfig,
            final long readTimeout,
            BiFunction<OidcTenantConfig, KeycloakPolicyEnforcerTenantConfig, Uni<KeycloakPolicyEnforcerContext>> policyConfigContextFactory) {
        this.defaultPolicyConfig = defaultPolicyConfig;
        this.staticPolicyConfig = staticPoliciesConfig;
        this.dynamicPolicyConfig = dynamicPoliciesConfig;
        this.policyConfigContextFactory = policyConfigContextFactory;
        this.readTimeout = readTimeout;
        this.staticOidcConfig = staticOidcConfig;
    }

    public Map<String, KeycloakPolicyEnforcerContext> getStaticPolicyConfig() {
        return staticPolicyConfig;
    }

    public KeycloakPolicyEnforcerContext getDefaultPolicyConfig() {
        return defaultPolicyConfig;
    }

    public BiFunction<OidcTenantConfig, KeycloakPolicyEnforcerTenantConfig, Uni<KeycloakPolicyEnforcerContext>> getPolicyConfigContextFactory() {
        return policyConfigContextFactory;
    }

    public Map<String, KeycloakPolicyEnforcerContext> getDynamicPolicyConfig() {
        return dynamicPolicyConfig;
    }

    public OidcTenantConfig getTenantOidcConfig(String tenantId) {
        return staticOidcConfig.namedTenants.get(tenantId);
    }

    public long getReadTimeout() {
        return readTimeout;
    }
}
