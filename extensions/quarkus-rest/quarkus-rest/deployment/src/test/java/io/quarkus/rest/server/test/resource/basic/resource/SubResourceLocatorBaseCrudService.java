package io.quarkus.rest.server.test.resource.basic.resource;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

public interface SubResourceLocatorBaseCrudService<T> {

    @GET
    @Path("/content/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    T getContent(
            @PathParam("id") String id);

    @POST
    @Path("/add")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    T add(T object);

    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    List<T> get();

    @PUT
    @Path("/update")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    T update(T object);

    @DELETE
    @Path("/delete/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    Boolean delete(
            @PathParam("id") String id);
}
