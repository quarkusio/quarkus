package io.quarkus.restclient.exception;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

@Path("/downstream")
public class DownstreamServiceUnavailableEndpoint {

    @GET
    public String getData() {
        throw new WebApplicationException(Response.status(503).header("Retry-After", "5").build());
    }
}
