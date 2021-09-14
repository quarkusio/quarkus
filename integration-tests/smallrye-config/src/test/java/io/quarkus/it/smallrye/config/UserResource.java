package io.quarkus.it.smallrye.config;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
