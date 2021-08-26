package io.quarkus.oidc.runtime;

import io.quarkus.oidc.OidcTenantConfig;

public class TenantConfigContext {

    /**
     * OIDC Provider
     */
    final OidcProvider provider;

    /**
     * Tenant configuration
     */
    final OidcTenantConfig oidcConfig;

    final boolean ready;

    public TenantConfigContext(OidcProvider client, OidcTenantConfig config) {
        this(client, config, true);
    }

    public TenantConfigContext(OidcProvider client, OidcTenantConfig config, boolean ready) {
        this.provider = client;
        this.oidcConfig = config;
        this.ready = ready;
    }

    public OidcTenantConfig getOidcTenantConfig() {
        return oidcConfig;
    }
}
