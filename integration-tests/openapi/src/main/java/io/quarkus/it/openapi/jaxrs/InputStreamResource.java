package io.quarkus.it.openapi.jaxrs;

import java.io.IOException;
import java.io.InputStream;
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
public class InputStreamResource {
    @GET
    @Path("/justInputStream/{fileName}")
    public InputStream justInputStream(@PathParam("fileName") String filename) {
        return toInputStream(filename);
    }

    @POST
    @Path("/justInputStream")
    public InputStream justInputStream(InputStream inputStream) {
        return inputStream;
    }

    @GET
    @Path("/restResponseInputStream/{fileName}")
    public RestResponse<InputStream> restResponseInputStream(@PathParam("fileName") String filename) {
        return RestResponse.ok(toInputStream(filename));
    }

    @POST
    @Path("/restResponseInputStream")
    public RestResponse<InputStream> restResponseInputStream(InputStream inputStream) {
        return RestResponse.ok(inputStream);
    }

    @GET
    @Path("/optionalInputStream/{fileName}")
    public Optional<InputStream> optionalInputStream(@PathParam("fileName") String filename) {
        return Optional.of(toInputStream(filename));
    }

    @POST
    @Path("/optionalInputStream")
    public Optional<InputStream> optionalInputStream(Optional<InputStream> inputStream) {
        return inputStream;
    }

    @GET
    @Path("/uniInputStream/{fileName}")
    public Uni<InputStream> uniInputStream(@PathParam("fileName") String filename) {
        return Uni.createFrom().item(toInputStream(filename));
    }

    @GET
    @Path("/completionStageInputStream/{fileName}")
    public CompletionStage<InputStream> completionStageInputStream(@PathParam("fileName") String filename) {
        return CompletableFuture.completedStage(toInputStream(filename));
    }

    @GET
    @Path("/completedFutureInputStream/{fileName}")
    public CompletableFuture<InputStream> completedFutureInputStream(@PathParam("fileName") String filename) {
        return CompletableFuture.completedFuture(toInputStream(filename));
    }

    private InputStream toInputStream(String filename) {
        try {
            String f = URLDecoder.decode(filename, "UTF-8");
            return Files.newInputStream(Paths.get(f));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
