package io.quarkus.it.smallrye.config;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/users")
@Transactional
public class UserResource {
    @Inject
    EntityManager entityManager;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response create() {
        User user = new User()
                .setFirstName("Naruto")
                .setLastName("Uzumaki")
                .setAge(17);
        entityManager.persist(user);
        return Response.ok().entity(user.getId()).build();
    }
}
