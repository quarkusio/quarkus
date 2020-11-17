package io.quarkus.hibernate.orm.sql_load_script;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import io.quarkus.hibernate.orm.MyEntity;

@Path("/orm-sql-load-script")
public class SqlLoadScriptTestResource {

    public static final String NO_ENTITY_MESSAGE = "no entity";

    @Inject
    EntityManager em;

    @GET
    @Path("/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getName(@PathParam("id") long id) {
        MyEntity entity = em.find(MyEntity.class, id);
        if (entity != null) {
            return entity.getName();
        }

        return NO_ENTITY_MESSAGE;
    }
}
