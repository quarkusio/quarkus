package io.quarkus.it.micrometer.prometheus;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/secured/{id}")
public class SecuredResourceB {

    @GET
    @Path("/details")
    public String details(@PathParam("id") String id) {
        return "details for " + id;
    }
}
