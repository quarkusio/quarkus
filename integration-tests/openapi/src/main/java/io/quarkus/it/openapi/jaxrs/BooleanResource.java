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
public class BooleanResource {
    @GET
    @Path("/justBoolean")
    public Boolean justBoolean() {
        return Boolean.TRUE;
    }

    @POST
    @Path("/justBoolean")
    public Boolean justBoolean(Boolean body) {
        return body;
    }

    @GET
    @Path("/justBool")
    public boolean justBool() {
        return true;
    }

    @POST
    @Path("/justBool")
    public boolean justBool(boolean body) {
        return body;
    }

    @GET
    @Path("/restResponseBoolean")
    public RestResponse<Boolean> restResponseBoolean() {
        return RestResponse.ok(Boolean.TRUE);
    }

    @POST
    @Path("/restResponseBoolean")
    public RestResponse<Boolean> restResponseBoolean(Boolean body) {
        return RestResponse.ok(body);
    }

    @GET
    @Path("/optionalBoolean")
    public Optional<Boolean> optionalBoolean() {
        return Optional.of(Boolean.TRUE);
    }

    @POST
    @Path("/optionalBoolean")
    public Optional<Boolean> optionalBoolean(Optional<Boolean> body) {
        return body;
    }

    @GET
    @Path("/uniBoolean")
    public Uni<Boolean> uniBoolean() {
        return Uni.createFrom().item(Boolean.TRUE);
    }

    @GET
    @Path("/completionStageBoolean")
    public CompletionStage<Boolean> completionStageBoolean() {
        return CompletableFuture.completedStage(Boolean.TRUE);
    }

    @GET
    @Path("/completedFutureBoolean")
    public CompletableFuture<Boolean> completedFutureBoolean() {
        return CompletableFuture.completedFuture(Boolean.TRUE);
    }

    @GET
    @Path("/listBoolean")
    public List<Boolean> listBoolean() {
        return Arrays.asList(new Boolean[] { Boolean.TRUE });
    }

    @POST
    @Path("/listBoolean")
    public List<Boolean> listBoolean(List<Boolean> body) {
        return body;
    }

    @GET
    @Path("/arrayBoolean")
    public Boolean[] arrayBoolean() {
        return new Boolean[] { Boolean.TRUE };
    }

    @POST
    @Path("/arrayBoolean")
    public Boolean[] arrayBoolean(Boolean[] body) {
        return body;
    }

    @GET
    @Path("/arrayBool")
    public boolean[] arrayBool() {
        return new boolean[] { true };
    }

    @POST
    @Path("/arrayBool")
    public boolean[] arrayBool(boolean[] body) {
        return body;
    }

    @GET
    @Path("/mapBoolean")
    public Map<Boolean, Boolean> mapBoolean() {
        Map<Boolean, Boolean> m = new HashMap<>();
        m.put(true, true);
        return m;
    }

    @POST
    @Path("/mapBoolean")
    public Map<Boolean, Boolean> mapBoolean(Map<Boolean, Boolean> body) {
        return body;
    }
}
