package io.quarkus.smallrye.opentracing.deployment;

import java.util.List;
import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public interface RestService {

    @GET
    @Path("/hello")
    Response hello();

    @GET
    @Path("/cdi")
    Response cdi();

    @GET
    @Path("/restClient")
    Response restClient();

    @GET
    @Path("/faultTolerance")
    CompletionStage<String> faultTolerance();

    @GET
    @Path("/jpa")
    @Produces(MediaType.APPLICATION_JSON)
    List<Fruit> jpa();
}
