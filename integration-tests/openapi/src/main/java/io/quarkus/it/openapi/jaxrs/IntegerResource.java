package io.quarkus.it.openapi.jaxrs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestResponse;

import io.smallrye.mutiny.Uni;

@Path("/jax-rs/defaultContentType")
public class IntegerResource {
    @GET
    @Path("/justInteger")
    public Integer justInteger() {
        return 0;
    }

    @POST
    @Path("/justInteger")
    public Integer justInteger(Integer body) {
        return body;
    }

    @GET
    @Path("/justInt")
    public int justInt() {
        return 0;
    }

    @POST
    @Path("/justInt")
    public int justInt(int body) {
        return body;
    }

    @GET
    @Path("/restResponseInteger")
    public RestResponse<Integer> restResponseInteger() {
        return RestResponse.ok(0);
    }

    @POST
    @Path("/restResponseInteger")
    public RestResponse<Integer> restResponseInteger(Integer body) {
        return RestResponse.ok(body);
    }

    @GET
    @Path("/optionalInteger")
    public Optional<Integer> optionalInteger() {
        return Optional.of(0);
    }

    @POST
    @Path("/optionalInteger")
    public Optional<Integer> optionalInteger(Optional<Integer> body) {
        return body;
    }

    @GET
    @Path("/optionalInt")
    public OptionalInt optionalInt() {
        return OptionalInt.of(0);
    }

    @POST
    @Path("/optionalInt")
    public OptionalInt optionalInt(OptionalInt body) {
        return body;
    }

    @GET
    @Path("/uniInteger")
    public Uni<Integer> uniInteger() {
        return Uni.createFrom().item(0);
    }

    @GET
    @Path("/completionStageInteger")
    public CompletionStage<Integer> completionStageInteger() {
        return CompletableFuture.completedStage(0);
    }

    @GET
    @Path("/completedFutureInteger")
    public CompletableFuture<Integer> completedFutureInteger() {
        return CompletableFuture.completedFuture(0);
    }

    @GET
    @Path("/listInteger")
    public List<Integer> listInteger() {
        return Arrays.asList(new Integer[] { 0 });
    }

    @POST
    @Path("/listInteger")
    public List<Integer> listInteger(List<Integer> body) {
        return body;
    }

    @GET
    @Path("/arrayInteger")
    public Integer[] arrayInteger() {
        return new Integer[] { 0 };
    }

    @POST
    @Path("/arrayInteger")
    public Integer[] arrayInteger(Integer[] body) {
        return body;
    }

    @GET
    @Path("/arrayInt")
    public int[] arrayInt() {
        return new int[] { 0 };
    }

    @POST
    @Path("/arrayInt")
    public int[] arrayInt(int[] body) {
        return body;
    }

    @GET
    @Path("/mapInteger")
    public Map<Integer, Integer> mapInteger() {
        Map<Integer, Integer> m = new HashMap<>();
        m.put(0, 0);
        return m;
    }

    @POST
    @Path("/mapInteger")
    public Map<Integer, Integer> mapInteger(Map<Integer, Integer> body) {
        return body;
    }
}
