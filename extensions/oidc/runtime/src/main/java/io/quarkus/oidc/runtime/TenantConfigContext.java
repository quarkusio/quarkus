package io.quarkus.oidc.runtime;

import io.vertx.ext.auth.oauth2.OAuth2Auth;

class TenantConfigContext {

    final OAuth2Auth auth;
    final OidcTenantConfig oidcConfig;

    TenantConfigContext(OAuth2Auth auth, OidcTenantConfig config) {
        this.auth = auth;
        oidcConfig = config;
    }

}
