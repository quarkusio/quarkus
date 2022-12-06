package io.quarkus.rest.client.reactive.redirect;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("/redirect")
public interface RedirectingResourceClient {
    @GET
    Response call(@QueryParam("redirects") Integer numberOfRedirects);
}
