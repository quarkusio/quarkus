package io.quarkus.keycloak.pep.runtime;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class KeycloakPolicyEnforcerRecorder {

    public void setup(OidcConfig oidcConfig, KeycloakPolicyEnforcerConfig config, BeanContainer beanContainer) {
        beanContainer.instance(KeycloakPolicyEnforcerAuthorizer.class).init(oidcConfig, config);
    }
}
