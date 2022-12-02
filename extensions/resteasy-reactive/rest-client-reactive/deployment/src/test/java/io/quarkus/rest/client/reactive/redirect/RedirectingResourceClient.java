package io.quarkus.rest.client.reactive.redirect;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/redirect")
public interface RedirectingResourceClient {
    @GET
    Response call(@QueryParam("redirects") Integer numberOfRedirects);
}
