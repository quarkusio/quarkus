package io.quarkus.it.panache.resources;

import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkus.it.panache.UserRepository;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {
    @Inject
    UserRepository userRepository;

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") final String id) {
        return userRepository.find(id)
                .map(user -> Response.ok(user).build())
                .orElse(Response.status(NOT_FOUND).build());
    }
}
