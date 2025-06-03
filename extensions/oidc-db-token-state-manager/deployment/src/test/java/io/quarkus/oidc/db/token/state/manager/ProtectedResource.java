package io.quarkus.oidc.db.token.state.manager;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.AuthorizationCodeTokens;
import io.quarkus.oidc.IdToken;
import io.quarkus.security.Authenticated;
import io.vertx.ext.web.RoutingContext;

@Path("/protected")
@Authenticated
public class ProtectedResource {

    @Inject
    @IdToken
    JsonWebToken idToken;

    @Inject
    RoutingContext context;

    @GET
    public String getName() {
        AuthorizationCodeTokens tokens = context.get(AuthorizationCodeTokens.class.getName());
        return idToken.getName()
                + ", access token: " + (tokens.getAccessToken() != null)
                + ", access_token_expires_in: " + (tokens.getAccessTokenExpiresIn() != null)
                + ", access_token_scope: " + (tokens.getAccessTokenScope() != null)
                + ", refresh_token: " + (tokens.getRefreshToken() != null);

    }

    @GET
    @Path("logout")
    public void logout() {
        throw new RuntimeException("Logout must be handled by CodeAuthenticationMechanism");
    }

}
