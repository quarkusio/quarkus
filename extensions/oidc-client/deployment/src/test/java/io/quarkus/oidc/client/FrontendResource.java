package io.quarkus.oidc.client;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
