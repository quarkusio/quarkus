package io.quarkus.oidc.runtime;

import io.quarkus.oidc.OidcTenantConfig;

class TenantConfigContext {

    /**
     * OIDC Provider
     */
    final OidcProvider provider;

    /**
     * Tenant configuration
     */
    final OidcTenantConfig oidcConfig;

    public TenantConfigContext(OidcProvider client, OidcTenantConfig config) {
        this.provider = client;
        this.oidcConfig = config;
    }
}
