package io.quarkus.resteasy.mutiny.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.annotations.Stream;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

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

    @Path("web-failure")
    @GET
    public Uni<String> failing() {
        return Uni.createFrom().item("not ok")
                .onItem().failWith(s -> new WebApplicationException(
                        Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(s).build()));
    }

    @Path("app-failure")
    @GET
    public Uni<String> failingBecauseOfApplicationCode() {
        return Uni.createFrom().item("not ok")
                .onItem().transform(s -> {
                    throw new IllegalStateException("BOOM!");
                });
    }

    @Path("response/tea-pot")
    @GET
    public Uni<Response> teapot() {
        return Uni.createFrom().item(() -> Response.status(418).build());
    }

    @Path("response/no-content")
    @GET
    public Uni<Response> noContent() {
        return Uni.createFrom().item(() -> Response.noContent().build());
    }

    @Path("response/accepted")
    @GET
    public Uni<Response> accepted() {
        return Uni.createFrom().item(() -> Response.accepted("Hello").build());
    }

    @Path("response/conditional/{test}")
    @GET
    public Uni<Response> conditional(@PathParam("test") boolean test) {
        return Uni.createFrom().item(test)
                .map(b -> b ? Response.accepted() : Response.noContent())
                .map(Response.ResponseBuilder::build);
    }
}
