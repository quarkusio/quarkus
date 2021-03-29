package io.quarkus.oidc.client.filter;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

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
