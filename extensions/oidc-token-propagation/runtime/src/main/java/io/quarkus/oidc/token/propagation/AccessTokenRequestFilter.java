package io.quarkus.oidc.token.propagation;

import java.io.IOException;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.client.ClientRequestContext;

import io.quarkus.oidc.token.propagation.runtime.AbstractTokenRequestFilter;
import io.quarkus.security.credential.TokenCredential;

public class AccessTokenRequestFilter extends AbstractTokenRequestFilter {

    @Inject
    Instance<TokenCredential> accessToken;

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        if (verifyTokenInstance(requestContext, accessToken)) {
            propagateToken(requestContext, accessToken.get().getToken());
        }
    }
}
