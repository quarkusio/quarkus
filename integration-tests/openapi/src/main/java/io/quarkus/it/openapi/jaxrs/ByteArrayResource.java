package io.quarkus.it.openapi.jaxrs;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.jboss.resteasy.reactive.RestResponse;

import io.smallrye.mutiny.Uni;

@Path("/jax-rs/defaultContentType")
public class ByteArrayResource {
    @GET
    @Path("/justByteArray/{fileName}")
    public byte[] justByteArray(@PathParam("fileName") String filename) {
        return toByteArray(filename);
    }

    @POST
    @Path("/justByteArray")
    public byte[] justByteArray(byte[] bs) {
        return bs;
    }

    @GET
    @Path("/restResponseByteArray/{fileName}")
    public RestResponse<byte[]> restResponseByteArray(@PathParam("fileName") String filename) {
        return RestResponse.ok(toByteArray(filename));
    }

    @POST
    @Path("/restResponseByteArray")
    public RestResponse<byte[]> restResponseByteArray(byte[] bs) {
        return RestResponse.ok(bs);
    }

    @GET
    @Path("/optionalByteArray/{fileName}")
    public Optional<byte[]> optionalByteArray(@PathParam("fileName") String filename) {
        return Optional.of(toByteArray(filename));
    }

    @POST
    @Path("/optionalByteArray")
    public Optional<byte[]> optionalByteArray(Optional<byte[]> inputStream) {
        return inputStream;
    }

    @GET
    @Path("/uniByteArray/{fileName}")
    public Uni<byte[]> uniByteArray(@PathParam("fileName") String filename) {
        return Uni.createFrom().item(toByteArray(filename));
    }

    @GET
    @Path("/completionStageByteArray/{fileName}")
    public CompletionStage<byte[]> completionStageByteArray(@PathParam("fileName") String filename) {
        return CompletableFuture.completedStage(toByteArray(filename));
    }

    @GET
    @Path("/completedFutureByteArray/{fileName}")
    public CompletableFuture<byte[]> completedFutureByteArray(@PathParam("fileName") String filename) {
        return CompletableFuture.completedFuture(toByteArray(filename));
    }

    private byte[] toByteArray(String filename) {
        try {
            String f = URLDecoder.decode(filename, "UTF-8");
            return Files.readAllBytes(Paths.get(f));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
