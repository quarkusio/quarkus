package io.quarkus.it.keycloak;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/frontend")
public class FrontendResource {
    @Inject
    @RestClient
    JwtTokenPropagationService jwtTokenPropagationService;

    @Inject
    @RestClient
    AccessTokenPropagationService accessTokenPropagationService;

    @Inject
    @RestClient
    ServiceAccountService serviceAccountService;

    @GET
    @Path("jwt-token-propagation")
    @RolesAllowed("user")
    public String userNameJwtTokenPropagation() {
        return jwtTokenPropagationService.getUserName();
    }

    @GET
    @Path("access-token-propagation")
    @RolesAllowed("user")
    public Response userNameAccessTokenPropagation() {
        try {
            return Response.ok(accessTokenPropagationService.getUserName()).build();
        } catch (Exception ex) {
            return Response.serverError().entity(ex.getMessage()).build();
        }
    }

    @GET
    @Path("service-account")
    public String userNameServiceAccount() {
        return serviceAccountService.getUserName();
    }
}
