package io.quarkus.restclient.exception;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@Path("/downstream")
public class DownstreamServiceUnavailableEndpoint {

    @GET
    public String getData() {
        throw new WebApplicationException(Response.status(503).header("Retry-After", "5").build());
    }
}
