package io.quarkus.hibernate.orm.multiplepersistenceunits;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.hibernate.orm.multiplepersistenceunits.model.annotation.shared.SharedEntity;

@Path("/multiple-persistence-units/orm-sql-load-script")
public class MultiplePersistenceUnitsSqlLoadScriptTestResource {

    public static final String NO_ENTITY_MESSAGE = "no entity";

    @Inject
    EntityManager em;

    @GET
    @Path("/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getName(@PathParam("id") long id) {
        SharedEntity entity = em.find(SharedEntity.class, id);
        if (entity != null) {
            return entity.getName();
        }

        return NO_ENTITY_MESSAGE;
    }
}
