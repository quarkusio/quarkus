package io.quarkus.keycloak.pep.runtime;

import org.keycloak.adapters.authorization.PolicyEnforcer;

import io.quarkus.oidc.OidcTenantConfig;

public class KeycloakPolicyEnforcerContext {
    final PolicyEnforcer enforcer;

    final OidcTenantConfig oidcTenantConfig;

    final KeycloakPolicyEnforcerTenantConfig enforcerConfig;

    final boolean ready;

    public KeycloakPolicyEnforcerContext(PolicyEnforcer enforcer, OidcTenantConfig oidcTenantConfig,
            KeycloakPolicyEnforcerTenantConfig enforcerConfig) {
        this(enforcer, oidcTenantConfig, enforcerConfig, true);
    }

    public KeycloakPolicyEnforcerContext(PolicyEnforcer enforcer, OidcTenantConfig oidcTenantConfig,
            KeycloakPolicyEnforcerTenantConfig enforcerConfig, boolean ready) {
        this.enforcer = enforcer;
        this.enforcerConfig = enforcerConfig;
        this.oidcTenantConfig = oidcTenantConfig;
        this.ready = ready;
    }
}
