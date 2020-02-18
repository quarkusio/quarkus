package io.quarkus.resteasy.mutiny.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.Stream;

import io.quarkus.resteasy.mutiny.test.annotations.Async;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@Path("/")
public class MutinyResource {
    @Path("uni")
    @GET
    public Uni<String> uni() {
        return Uni.createFrom().item("hello");
    }

    @Produces(MediaType.APPLICATION_JSON)
    @Path("multi")
    @GET
    @Stream
    public Multi<String> multi() {
        return Multi.createFrom().items("hello", "world");
    }

    @Path("injection")
    @GET
    public Uni<Integer> injection(@Context Integer value) {
        return Uni.createFrom().item(value);
    }

    @Path("injection-async")
    @GET
    public Uni<Integer> injectionAsync(@Async @Context Integer value) {
        return Uni.createFrom().item(value);
    }
}
