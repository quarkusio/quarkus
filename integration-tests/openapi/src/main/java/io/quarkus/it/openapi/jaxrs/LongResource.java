package io.quarkus.it.openapi.jaxrs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestResponse;

import io.smallrye.mutiny.Uni;

@Path("/jax-rs/defaultContentType")
public class LongResource {
    @GET
    @Path("/justLong")
    public Long justLong() {
        return 0L;
    }

    @POST
    @Path("/justLong")
    public Long justLong(Long body) {
        return body;
    }

    @GET
    @Path("/justPrimitiveLong")
    public long justPrimitiveLong() {
        return 0L;
    }

    @POST
    @Path("/justPrimitiveLong")
    public long justPrimitiveLong(long body) {
        return body;
    }

    @GET
    @Path("/restResponseLong")
    public RestResponse<Long> restResponseLong() {
        return RestResponse.ok(0L);
    }

    @POST
    @Path("/restResponseLong")
    public RestResponse<Long> restResponseLong(Long body) {
        return RestResponse.ok(body);
    }

    @GET
    @Path("/optionalLong")
    public Optional<Long> optionalLong() {
        return Optional.of(0L);
    }

    @POST
    @Path("/optionalLong")
    public Optional<Long> optionalLong(Optional<Long> body) {
        return body;
    }

    @GET
    @Path("/optionalPrimitiveLong")
    public OptionalLong optionalPrimitiveLong() {
        return OptionalLong.of(0L);
    }

    @POST
    @Path("/optionalPrimitiveLong")
    public OptionalLong optionalPrimitiveLong(OptionalLong body) {
        return body;
    }

    @GET
    @Path("/uniLong")
    public Uni<Long> uniLong() {
        return Uni.createFrom().item(0L);
    }

    @GET
    @Path("/completionStageLong")
    public CompletionStage<Long> completionStageLong() {
        return CompletableFuture.completedStage(0L);
    }

    @GET
    @Path("/completedFutureLong")
    public CompletableFuture<Long> completedFutureLong() {
        return CompletableFuture.completedFuture(0L);
    }

    @GET
    @Path("/listLong")
    public List<Long> listLong() {
        return Arrays.asList(new Long[] { 0L });
    }

    @POST
    @Path("/listLong")
    public List<Long> listLong(List<Long> body) {
        return body;
    }

    @GET
    @Path("/arrayLong")
    public Long[] arrayLong() {
        return new Long[] { 0L };
    }

    @POST
    @Path("/arrayLong")
    public Long[] arrayLong(Long[] body) {
        return body;
    }

    @GET
    @Path("/arrayPrimitiveLong")
    public long[] arrayPrimitiveLong() {
        return new long[] { 0L };
    }

    @POST
    @Path("/arrayPrimitiveLong")
    public long[] arrayPrimitiveLong(long[] body) {
        return body;
    }

    @GET
    @Path("/mapLong")
    public Map<Long, Long> mapLong() {
        Map<Long, Long> m = new HashMap<>();
        m.put(0L, 0L);
        return m;
    }

    @POST
    @Path("/mapLong")
    public Map<Long, Long> mapLong(Map<Long, Long> body) {
        return body;
    }
}
