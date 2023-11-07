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

import io.smallrye.mutiny.Uni;

@Path("/jax-rs/defaultContentType")
public class StringResource {

    @GET
    @Path("/justString")
    public String justString() {
        return "justString";
    }

    @POST
    @Path("/justString")
    public String justString(String body) {
        return body;
    }

    @GET
    @Path("/restResponseString")
    public RestResponse<String> restResponseString() {
        return RestResponse.ok("restResponseString");
    }

    @POST
    @Path("/restResponseString")
    public RestResponse<String> restResponseString(String body) {
        return RestResponse.ok(body);
    }

    @GET
    @Path("/optionalString")
    public Optional<String> optionalString() {
        return Optional.of("optionalString");
    }

    @POST
    @Path("/optionalString")
    public Optional<String> optionalString(Optional<String> body) {
        return body;
    }

    @GET
    @Path("/uniString")
    public Uni<String> uniString() {
        return Uni.createFrom().item("uniString");
    }

    @GET
    @Path("/completionStageString")
    public CompletionStage<String> completionStageString() {
        return CompletableFuture.completedStage("completionStageString");
    }

    @GET
    @Path("/completedFutureString")
    public CompletableFuture<String> completedFutureString() {
        return CompletableFuture.completedFuture("completedFutureString");
    }

    @GET
    @Path("/listString")
    public List<String> listString() {
        return Arrays.asList(new String[] { "listString" });
    }

    @POST
    @Path("/listString")
    public List<String> listString(List<String> body) {
        return body;
    }

    @GET
    @Path("/arrayString")
    public String[] arrayString() {
        return new String[] { "arrayString" };
    }

    @POST
    @Path("/arrayString")
    public String[] arrayString(String[] body) {
        return body;
    }

    @GET
    @Path("/mapString")
    public Map<String, String> mapString() {
        Map<String, String> m = new HashMap<>();
        m.put("mapString", "mapString");
        return m;
    }

    @POST
    @Path("/mapString")
    public Map<String, String> mapString(Map<String, String> body) {
        return body;
    }
}
