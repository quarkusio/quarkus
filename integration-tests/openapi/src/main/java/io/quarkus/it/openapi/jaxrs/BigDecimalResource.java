package io.quarkus.it.openapi.jaxrs;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestResponse;

import io.smallrye.mutiny.Uni;

@Path("/jax-rs/defaultContentType")
public class BigDecimalResource {
    @GET
    @Path("/justBigDecimal")
    public BigDecimal justBigDecimal() {
        return new BigDecimal("0");
    }

    @POST
    @Path("/justBigDecimal")
    public BigDecimal justBigDecimal(BigDecimal body) {
        return body;
    }

    @GET
    @Path("/restResponseBigDecimal")
    public RestResponse<BigDecimal> restResponseBigDecimal() {
        return RestResponse.ok(new BigDecimal("0"));
    }

    @POST
    @Path("/restResponseBigDecimal")
    public RestResponse<BigDecimal> restResponseBigDecimal(BigDecimal body) {
        return RestResponse.ok(body);
    }

    @GET
    @Path("/optionalBigDecimal")
    public Optional<BigDecimal> optionalBigDecimal() {
        return Optional.of(new BigDecimal("0"));
    }

    @POST
    @Path("/optionalBigDecimal")
    public Optional<BigDecimal> optionalBigDecimal(Optional<BigDecimal> body) {
        return body;
    }

    @GET
    @Path("/uniBigDecimal")
    public Uni<BigDecimal> uniBigDecimal() {
        return Uni.createFrom().item(new BigDecimal("0"));
    }

    @GET
    @Path("/completionStageBigDecimal")
    public CompletionStage<BigDecimal> completionStageBigDecimal() {
        return CompletableFuture.completedStage(new BigDecimal("0"));
    }

    @GET
    @Path("/completedFutureBigDecimal")
    public CompletableFuture<BigDecimal> completedFutureBigDecimal() {
        return CompletableFuture.completedFuture(new BigDecimal("0"));
    }

    @GET
    @Path("/listBigDecimal")
    public List<BigDecimal> listBigDecimal() {
        return Arrays.asList(new BigDecimal[] { new BigDecimal("0") });
    }

    @POST
    @Path("/listBigDecimal")
    public List<BigDecimal> listBigDecimal(List<BigDecimal> body) {
        return body;
    }

    @GET
    @Path("/arrayBigDecimal")
    public BigDecimal[] arrayBigDecimal() {
        return new BigDecimal[] { new BigDecimal("0") };
    }

    @POST
    @Path("/arrayBigDecimal")
    public BigDecimal[] arrayBigDecimal(BigDecimal[] body) {
        return body;
    }

}
