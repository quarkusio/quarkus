package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.common.ClientAssertionProvider;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig;

@Unremovable
@ApplicationScoped
class CustomClientAssertionProvider implements ClientAssertionProvider {

    @Override
    public String getClientAssertion() {
        return "123456";
    }

    @Override
    public boolean appliesTo(String name, OidcClientCommonConfig.Credentials.Jwt.Source source) {
        return "jwtbearer".equals(name);
    }

}
