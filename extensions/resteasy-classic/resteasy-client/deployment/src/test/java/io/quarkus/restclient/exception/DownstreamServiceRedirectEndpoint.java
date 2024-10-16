package io.quarkus.restclient.exception;

import java.net.URI;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.RedirectionException;
import jakarta.ws.rs.core.Response;

@Path("/downstream")
public class DownstreamServiceRedirectEndpoint {

    @GET
    public String getData() {
        throw new RedirectionException(
                Response.status(302).location(URI.create("http://localhost/private-service")).build());
    }
}
