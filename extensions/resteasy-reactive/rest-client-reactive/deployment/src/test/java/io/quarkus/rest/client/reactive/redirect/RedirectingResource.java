package io.quarkus.rest.client.reactive.redirect;

import java.net.URI;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("/redirect")
public class RedirectingResource {

    @GET
    public Response redirectedResponse(@QueryParam("redirects") Integer number) {
        if (number == null || 0 == number) {
            return Response.ok().build();
        } else {
            return Response.temporaryRedirect(URI.create("/redirect?redirects=" + (number - 1))).build();
        }
    }
}
