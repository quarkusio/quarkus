package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.IdToken;
import io.quarkus.oidc.runtime.DefaultTokenIntrospectionUserInfoCache;
import io.quarkus.security.Authenticated;
import io.vertx.ext.web.RoutingContext;

@Path("/code-flow-verify-id-and-access-tokens")
public class CodeFlowVerifyIdAndAccessTokenResource {

    @Inject
    @IdToken
    JsonWebToken idToken;

    @Inject
    JsonWebToken accessToken;

    @Inject
    RoutingContext routingContext;

    @Inject
    DefaultTokenIntrospectionUserInfoCache tokenCache;

    @GET
    @Authenticated
    public String access() {
        return "access token verified: " + (routingContext.get("code_flow_access_token_result") != null)
                + ", id_token issuer: " + idToken.getIssuer()
                + ", access_token issuer: " + accessToken.getIssuer()
                + ", id_token audience: " + String.join(";", idToken.getAudience().stream().sorted().toList())
                + ", access_token audience: " + accessToken.getAudience().iterator().next()
                + ", cache size: " + tokenCache.getCacheSize();
    }
}
