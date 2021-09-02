package io.quarkus.oidc.token.propagation;

import java.io.IOException;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.client.ClientRequestContext;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.oidc.token.propagation.runtime.AbstractTokenRequestFilter;
import io.smallrye.jwt.build.Jwt;

public class JsonWebTokenRequestFilter extends AbstractTokenRequestFilter {

    // note: We can't use constructor injection for these fields because they are registered by RESTEasy
    // which doesn't know about CDI at the point of registration

    @Inject
    Instance<org.eclipse.microprofile.jwt.JsonWebToken> jwtAccessToken;

    @Inject
    @ConfigProperty(name = "quarkus.oidc-token-propagation.secure-json-web-token")
    boolean resignToken;

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        if (verifyTokenInstance(requestContext, jwtAccessToken)) {
            propagateToken(requestContext, getToken());
        }
    }

    private String getToken() {
        if (resignToken) {
            return Jwt.claims(jwtAccessToken.get()).sign();
        } else {
            return jwtAccessToken.get().getRawToken();
        }
    }
}
