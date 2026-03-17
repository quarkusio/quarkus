package io.quarkus.it.micrometer.prometheus;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/secured/{id}/info")
public class SecuredResourceC {

    @GET
    @Path("/{section}")
    public String info(@PathParam("id") String id, @PathParam("section") String section) {
        return "info " + section + " for " + id;
    }
}
