package io.quarkus.rest.server.test.resource.basic.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

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
