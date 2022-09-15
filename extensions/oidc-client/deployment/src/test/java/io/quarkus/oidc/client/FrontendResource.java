package io.quarkus.oidc.client;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/frontend")
public class FrontendResource {

    @Inject
    @RestClient
    ProtectedResourceService protectedResourceService;

    @GET
    @Path("user")
    public String user() {
        return protectedResourceService.getUserName();
    }

}
