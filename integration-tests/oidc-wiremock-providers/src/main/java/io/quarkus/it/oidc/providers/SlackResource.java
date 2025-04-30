package io.quarkus.it.oidc.providers;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.SecurityContext;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.AuthorizationCodeFlow;
import io.quarkus.oidc.IdToken;

@Path("slack")
public class SlackResource {

    public record ProvidersResponseDto(String userPrincipalName, String userInfoEmail) {
    }

    @Inject
    @IdToken
    JsonWebToken idToken;

    @AuthorizationCodeFlow
    @GET
    public ProvidersResponseDto getPrincipalAndEmailFromSlackProvider(SecurityContext securityContext) {
        return new ProvidersResponseDto(securityContext.getUserPrincipal().getName(), idToken.getClaim("email"));
    }

}
