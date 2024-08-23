package io.quarkus.it.spring.data.jpa.complex;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/complex/parents")
public class ParentResource {
    private final ParentMidRepository<Parent> parentRepository;

    public ParentResource(ParentMidRepository<Parent> parentRepository) {
        this.parentRepository = parentRepository;
    }

    @POST
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Parent createParent(@PathParam("id") Long parentId) {
        var parent = new Parent(parentId, "Test", null, 50, 0f, TestEnum.TEST);
        return parentRepository.save(parent);
    }

    @GET
    @Path("/p1/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getParent(@PathParam("id") Long parentId,
            @QueryParam("name") String name) {
        var data = parentRepository.findSomethingByIdAndName(parentId, name);
        if (data.isEmpty()) {
            return Response.noContent().build();
        } else {
            return Response.ok(data).build();
        }
    }

    @GET
    @Path("/p2/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getParent2(@PathParam("id") Long parentId,
            @QueryParam("name") String name) {
        var data = parentRepository.findSmallParent2ByIdAndName(parentId, name);
        if (data.isEmpty()) {
            return Response.noContent().build();
        } else {
            return Response.ok(data).build();
        }
    }

}
