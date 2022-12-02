package io.quarkus.it.keycloak;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

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
