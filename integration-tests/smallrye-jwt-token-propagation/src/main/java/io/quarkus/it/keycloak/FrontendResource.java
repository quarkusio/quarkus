package io.quarkus.it.keycloak;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/frontend")
public class FrontendResource {
    @Inject
    JsonWebToken jwt;

    @Inject
    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String configuredIssuer;

    @Inject
    @RestClient
    JwtTokenPropagationService jwtTokenPropagationService;

    @Inject
    @RestClient
    AccessTokenPropagationService accessTokenPropagationService;

    @GET
    @Path("jwt-token-propagation")
    @RolesAllowed("user")
    public String userNameJwtTokenPropagation() {
        checkIssuerAndAudience();
        return jwtTokenPropagationService.getUserName();
    }

    @GET
    @Path("access-token-propagation")
    @RolesAllowed("user")
    public String userNameAccessTokenPropagation() {
        checkIssuerAndAudience();
        return accessTokenPropagationService.getUserName();
    }

    @POST
    @Path("access-token-propagation")
    @RolesAllowed("user")
    @Consumes("application/json")
    public String userNameAccessTokenPropagationPost(User user) {
        checkIssuerAndAudience();
        return accessTokenPropagationService.echoUserName(user.getName());
    }

    private void checkIssuerAndAudience() {
        // it has already been verified by smallrye-jwt
        if (!configuredIssuer.equals(jwt.getIssuer())) {
            throw new NotAuthorizedException(401);
        }
        if (jwt.getAudience() != null) {
            // Keycloak does not set the audience
            throw new NotAuthorizedException(401);
        }
    }
}
