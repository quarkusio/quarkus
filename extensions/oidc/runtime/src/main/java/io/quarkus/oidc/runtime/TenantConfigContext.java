package io.quarkus.oidc.runtime;

import io.quarkus.oidc.OidcTenantConfig;
import io.vertx.ext.auth.oauth2.OAuth2Auth;

class TenantConfigContext {

    /**
     * Discovered OIDC
     */
    final OAuth2Auth auth;
    /**
     * Tenant configuration
     */
    final OidcTenantConfig oidcConfig;

    TenantConfigContext(OAuth2Auth auth, OidcTenantConfig config) {
        this.auth = auth;
        this.oidcConfig = config;
    }

}
