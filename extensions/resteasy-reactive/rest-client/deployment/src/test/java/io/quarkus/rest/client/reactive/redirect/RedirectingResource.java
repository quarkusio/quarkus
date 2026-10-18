package io.quarkus.rest.client.reactive.redirect;

import java.net.URI;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

@Path("/redirect")
public class RedirectingResource {

    @GET
    @Path("302")
    public Response redirectedResponse(@QueryParam("redirects") Integer number, HttpHeaders httpHeaders) {
        if (number == null || 0 == number) {
            var builder = Response.ok();
            String fooHeader = httpHeaders.getHeaderString("x-foo");
            if (fooHeader != null) {
                builder.header("x-foo", fooHeader);
            }
            return builder.build();
        } else {
            return Response.status(Response.Status.FOUND).location(URI.create("/redirect/302?redirects=" + (number - 1)))
                    .build();
        }
    }

    @POST
    @Path("/302/post")
    public Response redirectedResponse() {
        // it redirects to the GET resource
        return Response.status(Response.Status.FOUND).location(URI.create("/redirect/302?redirects=0")).build();
    }

    @POST
    @Path("/307")
    public Response temporatyRedirectedResponse(@QueryParam("redirects") Integer number) {
        if (number == null || 0 == number) {
            return Response.ok().build();
        }
        // will redirect back to this method since the HTTP method should not change when returning 307
        return Response.status(Response.Status.TEMPORARY_REDIRECT).location(URI.create("/redirect/307?redirects=0")).build();
    }
}
