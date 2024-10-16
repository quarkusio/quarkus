package io.quarkus.rest.client.reactive.error.clientexceptionmapper;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/error")
public class Resource {

    @Path("/404")
    @GET
    public Response get404() {
        return Response.status(404).build();
    }

    @Path("/400")
    @GET
    public Response get400() {
        return Response.status(400).build();
    }
}
