package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

/**
 * This interface is used on the client side only.
 *
 * @author Jozef Hartinger
 */

@Path("/student/{id}")
@Produces("application/student")
@Consumes("application/student")
public interface GenericResourceStudentInterface {
    @GET
    GenericResourceStudent get(@PathParam("id") Integer id);

    @PUT
    void put(@PathParam("id") Integer id, GenericResourceStudent entity);
}
