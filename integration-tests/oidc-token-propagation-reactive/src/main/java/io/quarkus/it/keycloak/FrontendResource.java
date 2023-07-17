package io.quarkus.it.keycloak;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.smallrye.mutiny.Uni;

@Path("/frontend")
public class FrontendResource {
    @Inject
    @RestClient
    AccessTokenPropagationService accessTokenPropagationService;

    @Inject
    @RestClient
    ServiceWithoutToken serviceWithoutToken;

    @GET
    @Path("access-token-propagation")
    @Produces("text/plain")
    @RolesAllowed("user")
    public Uni<String> userNameAccessTokenPropagation() {
        return accessTokenPropagationService.getUserName();
    }

    @GET
    @Path("service-without-token")
    @Produces("text/plain")
    public Uni<String> userNameServiceWithoutToken() {
        return serviceWithoutToken.getUserName();
    }
}
