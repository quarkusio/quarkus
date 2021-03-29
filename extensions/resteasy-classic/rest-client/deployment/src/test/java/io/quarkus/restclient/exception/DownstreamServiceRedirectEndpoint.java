package io.quarkus.restclient.exception;

import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.core.Response;

@Path("/downstream")
public class DownstreamServiceRedirectEndpoint {

    @GET
    public String getData() {
        throw new RedirectionException(
                Response.status(302).location(URI.create("http://localhost/private-service")).build());
    }
}
