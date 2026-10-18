package io.quarkus.it.keycloak;

import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.security.Authenticated;

@Authenticated
@Path("/rar")
public class RarResource {

    @Any
    @Inject
    OidcTokenResponseFilter oidcTokenResponseFilter;

    @Path("/token-response/authorization-details")
    @GET
    public String getAuthorizationDetails() {
        return oidcTokenResponseFilter.getAuthorizationDetails();
    }

}
