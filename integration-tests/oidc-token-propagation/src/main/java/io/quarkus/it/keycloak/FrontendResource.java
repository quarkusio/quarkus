package io.quarkus.it.keycloak;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.security.Authenticated;

@Path("/frontend")
@Authenticated
public class FrontendResource {
    @Inject
    @RestClient
    ProtectedResourceService protectedResourceService;

    @GET
    @Path("user")
    @RolesAllowed("user")
    public String userName() {
        return protectedResourceService.getUserName();
    }
}
