package io.quarkus.smallrye.metrics.jaxrs;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;

@Path("/")
public class MetricsResource {

    @Path("/hello/{name}")
    @GET
    public String hello(@PathParam("name") String name) {
        return "hello " + name;
    }

    @Path("/error")
    @GET
    public Response error() {
        return Response.serverError().build();
    }

    @Path("/exception")
    @GET
    public Long exception() {
        throw new RuntimeException("!!!");
    }

    @GET
    @Path("{segment}/{other}/{segment}/list")
    public Response list(@PathParam("segment") List<PathSegment> segments) {
        return Response.ok().build();
    }

    @GET
    @Path("{segment}/{other}/{segment}/array")
    public Response array(@PathParam("segment") PathSegment[] segments) {
        return Response.ok().build();
    }

    @GET
    @Path("{segment}/{other}/{segment}/varargs")
    public Response varargs(@PathParam("segment") PathSegment... segments) {
        return Response.ok().build();
    }

    @Path("/async")
    @GET
    public CompletionStage<String> async() {
        return CompletableFuture.supplyAsync(() -> "Hello");
    }

}
