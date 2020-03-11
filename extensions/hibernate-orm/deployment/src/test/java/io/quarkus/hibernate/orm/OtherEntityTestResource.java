package io.quarkus.hibernate.orm;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
