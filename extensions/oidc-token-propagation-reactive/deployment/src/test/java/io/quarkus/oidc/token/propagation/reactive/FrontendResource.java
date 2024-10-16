package io.quarkus.oidc.token.propagation.reactive;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/frontend")
public class FrontendResource {
    @Inject
    @RestClient
    AccessTokenPropagationService accessTokenPropagationService;

    @Inject
    JsonWebToken jwt;

    @GET
    @Path("token-propagation")
    @RolesAllowed("admin")
    public String userNameTokenPropagation() {
        return getResponseWithExchangedUsername();
    }

    @GET
    @Path("token-propagation-with-augmentor")
    @RolesAllowed("Bearertester") // Bearertester role is granted by SecurityIdentityAugmentor
    public String userNameTokenPropagationWithSecIdentityAugmentor() {
        return getResponseWithExchangedUsername();
    }

    private String getResponseWithExchangedUsername() {
        if ("alice".equals(jwt.getName())) {
            return "Token issued to " + jwt.getName() + " has been exchanged, new user name: "
                    + accessTokenPropagationService.getUserName();
        } else {
            throw new RuntimeException();
        }
    }
}
