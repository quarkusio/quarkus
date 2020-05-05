package io.quarkus.smallrye.opentracing.deployment;

import java.util.List;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
