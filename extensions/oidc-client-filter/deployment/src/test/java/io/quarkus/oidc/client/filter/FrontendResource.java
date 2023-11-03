package io.quarkus.oidc.client.filter;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/frontend")
public class FrontendResource {

    @Inject
    @RestClient
    ProtectedResourceService protectedResourceService;

    @GET
    @Path("user-before-registering-provider")
    public String userBeforeRegisteringProvider() {
        try {
            protectedResourceService.getUserName();
            throw new InternalServerErrorException("HTTP 401 error is expected");
        } catch (WebApplicationException ex) {
            if (ex.getResponse().getStatus() == 401) {
                throw new NotAuthorizedException(
                        Response.status(401).entity("ProtectedResourceService requires a token").build());
            } else {
                throw new InternalServerErrorException("HTTP 401 error is expected");
            }
        }
    }

    @GET
    @Path("user-after-registering-provider")
    public String userAfterRegisteringProvider() {
        return protectedResourceService.getUserName();
    }
}
