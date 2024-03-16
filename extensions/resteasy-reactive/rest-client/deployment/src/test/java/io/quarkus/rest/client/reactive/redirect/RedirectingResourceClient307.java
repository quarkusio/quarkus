package io.quarkus.rest.client.reactive.redirect;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("/redirect/307")
public interface RedirectingResourceClient307 {
    /**
     * By default, the `quarkus.rest-client.follow-redirects` property only works in GET and HEAD resources, so POST resources
     * are never redirect, unless users register a custom {@link org.jboss.resteasy.reactive.client.handlers.RedirectHandler}.
     */
    @POST
    Response post(@QueryParam("redirects") Integer numberOfRedirects);
}
