package io.quarkus.it.micrometer.prometheus;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/secured")
public class SecuredResource {

    @GET
    @Path("item/{id}")
    public String item(@PathParam("id") String id) {
        return "return message with id " + id;
    }
}
