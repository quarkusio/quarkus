package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

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
