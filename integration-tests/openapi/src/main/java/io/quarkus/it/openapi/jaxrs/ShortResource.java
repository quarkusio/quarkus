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
public class ShortResource {
    @GET
    @Path("/justShort")
    public Short justShort() {
        return 0;
    }

    @POST
    @Path("/justShort")
    public Short justShort(Short body) {
        return body;
    }

    @GET
    @Path("/justPrimitiveShort")
    public short justPrimitiveShort() {
        return 0;
    }

    @POST
    @Path("/justPrimitiveShort")
    public int justPrimitiveShort(int body) {
        return body;
    }

    @GET
    @Path("/restResponseShort")
    public RestResponse<Short> restResponseShort() {
        return RestResponse.ok((short) 0);
    }

    @POST
    @Path("/restResponseShort")
    public RestResponse<Short> restResponseShort(Short body) {
        return RestResponse.ok(body);
    }

    @GET
    @Path("/optionalShort")
    public Optional<Short> optionalShort() {
        return Optional.of((short) 0);
    }

    @POST
    @Path("/optionalShort")
    public Optional<Short> optionalShort(Optional<Short> body) {
        return body;
    }

    @GET
    @Path("/uniShort")
    public Uni<Short> uniShort() {
        return Uni.createFrom().item((short) 0);
    }

    @GET
    @Path("/completionStageShort")
    public CompletionStage<Short> completionStageShort() {
        return CompletableFuture.completedStage((short) 0);
    }

    @GET
    @Path("/completedFutureShort")
    public CompletableFuture<Short> completedFutureShort() {
        return CompletableFuture.completedFuture((short) 0);
    }

    @GET
    @Path("/listShort")
    public List<Short> listShort() {
        return Arrays.asList(new Short[] { 0 });
    }

    @POST
    @Path("/listShort")
    public List<Short> listShort(List<Short> body) {
        return body;
    }

    @GET
    @Path("/arrayShort")
    public Short[] arrayShort() {
        return new Short[] { (short) 0 };
    }

    @POST
    @Path("/arrayShort")
    public Short[] arrayShort(Short[] body) {
        return body;
    }

    @GET
    @Path("/arrayPrimitiveShort")
    public short[] arrayPrimitiveShort() {
        return new short[] { (short) 0 };
    }

    @POST
    @Path("/arrayPrimitiveShort")
    public short[] arrayPrimitiveShort(short[] body) {
        return body;
    }

    @GET
    @Path("/mapShort")
    public Map<Short, Short> mapShort() {
        Map<Short, Short> m = new HashMap<>();
        m.put((short) 0, (short) 0);
        return m;
    }

    @POST
    @Path("/mapShort")
    public Map<Short, Short> mapShort(Map<Short, Short> body) {
        return body;
    }
}
