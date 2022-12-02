package io.quarkus.rest.client.reactive.redirect;

import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

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
