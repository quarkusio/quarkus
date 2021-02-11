package io.quarkus.it.keycloak;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/frontend")
public class FrontendResource {
    @Inject
    @RestClient
    TokenPropagationService tokenPropagationService;

    @Inject
    @RestClient
    ServiceAccountService serviceAccountService;

    @GET
    @Path("token-propagation")
    @RolesAllowed("user")
    public String userNameTokenPropagation() {
        return tokenPropagationService.getUserName();
    }

    @GET
    @Path("service-account")
    public String userNameServiceAccount() {
        return serviceAccountService.getUserName();
    }
}
