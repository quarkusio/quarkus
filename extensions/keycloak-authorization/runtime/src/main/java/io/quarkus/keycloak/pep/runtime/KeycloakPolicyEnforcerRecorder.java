package io.quarkus.keycloak.pep.runtime;

import java.util.function.Supplier;

import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.HttpConfiguration;

@Recorder
public class KeycloakPolicyEnforcerRecorder {

    public Supplier<KeycloakPolicyEnforcerConfigBean> setup(OidcConfig oidcConfig, KeycloakPolicyEnforcerConfig config,
            TlsConfig tlsConfig, HttpConfiguration httpConfiguration) {
        if (oidcConfig.defaultTenant.applicationType == OidcTenantConfig.ApplicationType.WEB_APP) {
            throw new OIDCException("Application type [" + oidcConfig.defaultTenant.applicationType + "] is not supported");
        }
        return new Supplier<KeycloakPolicyEnforcerConfigBean>() {
            @Override
            public KeycloakPolicyEnforcerConfigBean get() {
                return new KeycloakPolicyEnforcerConfigBean(oidcConfig, config, tlsConfig, httpConfiguration);
            }
        };
    }
}
