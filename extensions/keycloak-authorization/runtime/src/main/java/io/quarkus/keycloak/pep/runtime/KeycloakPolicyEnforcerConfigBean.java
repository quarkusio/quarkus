package io.quarkus.keycloak.pep.runtime;

import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.vertx.http.runtime.HttpConfiguration;

public class KeycloakPolicyEnforcerConfigBean {

    final OidcConfig oidcConfig;
    final KeycloakPolicyEnforcerConfig keycloakPolicyEnforcerConfig;
    final TlsConfig tlsConfig;
    final HttpConfiguration httpConfiguration;

    public KeycloakPolicyEnforcerConfigBean(OidcConfig oidcConfig, KeycloakPolicyEnforcerConfig keycloakPolicyEnforcerConfig,
            TlsConfig tlsConfig,
            HttpConfiguration httpConfiguration) {
        this.oidcConfig = oidcConfig;
        this.keycloakPolicyEnforcerConfig = keycloakPolicyEnforcerConfig;
        this.tlsConfig = tlsConfig;
        this.httpConfiguration = httpConfiguration;
    }
}
