package io.quarkus.it.envers;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestStreamElementType;

import io.smallrye.mutiny.Multi;

@Path("output")
public class OutputResource {

    @GET
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    @CustomOutput("dummy")
    @Path("annotation")
    public Multi<Message> sseOut() {
        return Multi.createFrom().iterable(List.of(new Message("test"), new Message("test")));
    }

    @GET
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    @Path("no-annotation")
    public Multi<Message> sseOut2() {
        return Multi.createFrom().iterable(List.of(new Message("test"), new Message("test")));
    }
}
