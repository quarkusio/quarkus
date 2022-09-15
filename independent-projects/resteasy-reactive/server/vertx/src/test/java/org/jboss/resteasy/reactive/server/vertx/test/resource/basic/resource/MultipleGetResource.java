package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

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
