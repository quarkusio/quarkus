package io.quarkus.it.openapi.jaxrs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestResponse;

import io.quarkus.it.openapi.Greeting;
import io.smallrye.mutiny.Uni;

@Path("/jax-rs/defaultContentType")
public class PojoResource {

    @GET
    @Path("/justPojo")
    public Greeting justPojo() {
        return new Greeting(0, "justPojo");
    }

    @POST
    @Path("/justPojo")
    public Greeting justPojo(Greeting greeting) {
        return greeting;
    }

    @GET
    @Path("/restResponsePojo")
    public RestResponse<Greeting> restResponsePojo() {
        return RestResponse.ok(new Greeting(0, "restResponsePojo"));
    }

    @POST
    @Path("/restResponsePojo")
    public RestResponse<Greeting> restResponsePojo(Greeting greeting) {
        return RestResponse.ok(greeting);
    }

    @GET
    @Path("/optionalPojo")
    public Optional<Greeting> optionalPojo() {
        return Optional.of(new Greeting(0, "optionalPojo"));
    }

    @POST
    @Path("/optionalPojo")
    public Optional<Greeting> optionalPojo(Optional<Greeting> greeting) {
        return greeting;
    }

    @GET
    @Path("/uniPojo")
    public Uni<Greeting> uniPojo() {
        return Uni.createFrom().item(new Greeting(0, "uniPojo"));
    }

    @GET
    @Path("/completionStagePojo")
    public CompletionStage<Greeting> completionStagePojo() {
        return CompletableFuture.completedStage(new Greeting(0, "completionStagePojo"));
    }

    @GET
    @Path("/completedFuturePojo")
    public CompletableFuture<Greeting> completedFuturePojo() {
        return CompletableFuture.completedFuture(new Greeting(0, "completedFuturePojo"));
    }

    @GET
    @Path("/listPojo")
    public List<Greeting> listPojo() {
        return Arrays.asList(new Greeting[] { new Greeting(0, "listPojo") });
    }

    @POST
    @Path("/listPojo")
    public List<Greeting> listPojo(List<Greeting> greeting) {
        return greeting;
    }

    @GET
    @Path("/arrayPojo")
    public Greeting[] arrayPojo() {
        return new Greeting[] { new Greeting(0, "arrayPojo") };
    }

    @POST
    @Path("/arrayPojo")
    public Greeting[] arrayPojo(Greeting[] greeting) {
        return greeting;
    }

    @GET
    @Path("/mapPojo")
    public Map<String, Greeting> mapPojo() {
        Map<String, Greeting> m = new HashMap<>();
        Greeting g = new Greeting(0, "mapPojo");
        m.put("mapPojo", g);
        return m;
    }

    @POST
    @Path("/mapPojo")
    public Map<String, Greeting> mapPojo(Map<String, Greeting> body) {
        return body;
    }
}
