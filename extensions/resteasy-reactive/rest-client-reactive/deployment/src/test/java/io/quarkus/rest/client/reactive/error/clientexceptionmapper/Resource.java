package io.quarkus.rest.client.reactive.error.clientexceptionmapper;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

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
