package io.quarkus.it.openapi.jaxrs;

import java.math.BigInteger;
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
public class BigIntegerResource {
    @GET
    @Path("/justBigInteger")
    public BigInteger justBigInteger() {
        return new BigInteger("0");
    }

    @POST
    @Path("/justBigInteger")
    public BigInteger justBigInteger(BigInteger body) {
        return body;
    }

    @GET
    @Path("/restResponseBigInteger")
    public RestResponse<BigInteger> restResponseBigInteger() {
        return RestResponse.ok(new BigInteger("0"));
    }

    @POST
    @Path("/restResponseBigInteger")
    public RestResponse<BigInteger> restResponseBigInteger(BigInteger body) {
        return RestResponse.ok(body);
    }

    @GET
    @Path("/optionalBigInteger")
    public Optional<BigInteger> optionalBigInteger() {
        return Optional.of(new BigInteger("0"));
    }

    @POST
    @Path("/optionalBigInteger")
    public Optional<BigInteger> optionalBigInteger(Optional<BigInteger> body) {
        return body;
    }

    @GET
    @Path("/uniBigInteger")
    public Uni<BigInteger> uniBigInteger() {
        return Uni.createFrom().item(new BigInteger("0"));
    }

    @GET
    @Path("/completionStageBigInteger")
    public CompletionStage<BigInteger> completionStageBigInteger() {
        return CompletableFuture.completedStage(new BigInteger("0"));
    }

    @GET
    @Path("/completedFutureBigInteger")
    public CompletableFuture<BigInteger> completedFutureBigInteger() {
        return CompletableFuture.completedFuture(new BigInteger("0"));
    }

    @GET
    @Path("/listBigInteger")
    public List<BigInteger> listBigInteger() {
        return Arrays.asList(new BigInteger[] { new BigInteger("0") });
    }

    @POST
    @Path("/listBigInteger")
    public List<BigInteger> listBigInteger(List<BigInteger> body) {
        return body;
    }

    @GET
    @Path("/arrayBigInteger")
    public BigInteger[] arrayBigInteger() {
        return new BigInteger[] { new BigInteger("0") };
    }

    @POST
    @Path("/arrayBigInteger")
    public BigInteger[] arrayBigInteger(BigInteger[] body) {
        return body;
    }

}
