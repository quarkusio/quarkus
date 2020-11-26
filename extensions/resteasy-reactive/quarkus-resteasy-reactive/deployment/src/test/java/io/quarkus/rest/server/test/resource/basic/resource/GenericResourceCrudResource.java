package io.quarkus.rest.server.test.resource.basic.resource;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * This is a sample of a CRUD resource template which can be reused for different entities.
 *
 * @param <ENTITY_TYPE> Type of the entity which CRUD operations are performed on.
 * @param <ENTITY_IDENTIFIER_TYPE> Type of the entity identified i.e. java.lang.Long
 * @author Jozef Hartinger
 */
public abstract class GenericResourceCrudResource<ENTITY_TYPE, ENTITY_IDENTIFIER_TYPE> {
    abstract ENTITY_TYPE getEntity(ENTITY_IDENTIFIER_TYPE id);

    abstract void setEntity(ENTITY_IDENTIFIER_TYPE id, ENTITY_TYPE entity);

    @GET
    @Path("/{id}")
    public ENTITY_TYPE get(@PathParam("id") ENTITY_IDENTIFIER_TYPE id) {
        return getEntity(id);
    }

    @PUT
    @Path("/{id}")
    public void put(@PathParam("id") ENTITY_IDENTIFIER_TYPE id, ENTITY_TYPE entity) {
        setEntity(id, entity);
    }
}
