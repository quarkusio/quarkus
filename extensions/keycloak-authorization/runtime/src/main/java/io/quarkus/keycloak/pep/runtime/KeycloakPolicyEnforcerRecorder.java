package io.quarkus.keycloak.pep.runtime;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.HttpConfiguration;

@Recorder
public class KeycloakPolicyEnforcerRecorder {

    public void setup(OidcConfig oidcConfig, KeycloakPolicyEnforcerConfig config, BeanContainer beanContainer,
            HttpConfiguration httpConfiguration) {
        if (oidcConfig.defaultTenant.applicationType == OidcTenantConfig.ApplicationType.WEB_APP) {
            throw new OIDCException("Application type [" + oidcConfig.defaultTenant.applicationType + "] is not supported");
        }
        beanContainer.instance(KeycloakPolicyEnforcerAuthorizer.class).init(oidcConfig, config, httpConfiguration);
    }
}
