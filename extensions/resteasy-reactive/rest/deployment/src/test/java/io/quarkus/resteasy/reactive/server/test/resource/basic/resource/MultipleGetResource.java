package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MultipleGetResource {
    private List<String> todoList = new ArrayList<>();

    @GET
    public List<String> getAll() {
        return todoList;
    }

    @GET
    public List<String> findNotCompleted() {
        return todoList;
    }
}
