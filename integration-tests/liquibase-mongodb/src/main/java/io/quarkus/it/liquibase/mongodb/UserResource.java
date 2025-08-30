package io.quarkus.it.liquibase.mongodb;

import java.util.List;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import org.bson.types.ObjectId;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    @GET
    public List<User> list() {
        return User.listAll();
    }

    @GET
    @Path("/{id}")
    public User get(@PathParam("id") String id) {
        return User.findById(new ObjectId(id));
    }

    @POST
    public void save(User user) {
        user.persist();
    }
}
