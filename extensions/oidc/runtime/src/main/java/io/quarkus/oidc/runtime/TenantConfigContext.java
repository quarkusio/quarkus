package io.quarkus.oidc.runtime;

import io.quarkus.oidc.OidcTenantConfig;

class TenantConfigContext {

    /**
     * OIDC Runtime client
     */
    final OidcRuntimeClient client;

    /**
     * Tenant configuration
     */
    final OidcTenantConfig oidcConfig;

    public TenantConfigContext(OidcRuntimeClient client, OidcTenantConfig config) {
        this.client = client;
        this.oidcConfig = config;
    }
}
