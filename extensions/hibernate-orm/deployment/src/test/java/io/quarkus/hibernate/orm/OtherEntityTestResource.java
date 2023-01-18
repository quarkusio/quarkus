package io.quarkus.hibernate.orm;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/other-entity")
public class OtherEntityTestResource {

    @Inject
    EntityManager em;

    @GET
    @Path("/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getName(@PathParam("id") long id) {
        OtherEntity entity = em.find(OtherEntity.class, id);
        if (entity != null) {
            return entity.toString();
        }

        return "no entity";
    }
}
