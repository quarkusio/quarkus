package io.quarkus.rest.client.reactive.redirect;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("/redirect/302")
public interface RedirectingResourceClient302 {
    @GET
    Response call(@QueryParam("redirects") Integer numberOfRedirects);

    /**
     * By default, the `quarkus.rest-client.follow-redirects` property only works in GET and HEAD resources, so POST resources
     * are never redirect, unless users register a custom {@link org.jboss.resteasy.reactive.client.handlers.RedirectHandler}.
     */
    @POST
    @Path("/post")
    Response post();
}
