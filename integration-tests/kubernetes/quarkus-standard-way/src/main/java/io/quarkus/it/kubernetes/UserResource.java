package io.quarkus.it.kubernetes;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/users")
public class UserResource {

    @GET
    @Path("{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getUser(@PathParam("id") String id) {
        return id;
    }

    @PUT
    @Path("{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String updateUser(@PathParam("id") String id) {
        return id;
    }

    @DELETE
    @Path("{id}")
    public void deleteUser(@PathParam("id") String id) {
    }

    @GET
    @Path("{id}/orders")
    @Produces(MediaType.TEXT_PLAIN)
    public String getUserOrders(@PathParam("id") String id) {
        return id;
    }
}
