package io.quarkus.it.openapi.jaxrs;

import java.io.IOException;
import java.io.Reader;
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
public class ReaderResource {
    @GET
    @Path("/justReader/{fileName}")
    public Reader justReader(@PathParam("fileName") String filename) {
        return toReader(filename);
    }

    @POST
    @Path("/justReader")
    public Reader justReader(Reader inputStream) {
        return inputStream;
    }

    @GET
    @Path("/restResponseReader/{fileName}")
    public RestResponse<Reader> restResponseReader(@PathParam("fileName") String filename) {
        return RestResponse.ok(toReader(filename));
    }

    @POST
    @Path("/restResponseReader")
    public RestResponse<Reader> restResponseReader(Reader inputStream) {
        return RestResponse.ok(inputStream);
    }

    @GET
    @Path("/optionalReader/{fileName}")
    public Optional<Reader> optionalReader(@PathParam("fileName") String filename) {
        return Optional.of(toReader(filename));
    }

    @POST
    @Path("/optionalReader")
    public Optional<Reader> optionalReader(Optional<Reader> inputStream) {
        return inputStream;
    }

    @GET
    @Path("/uniReader/{fileName}")
    public Uni<Reader> uniReader(@PathParam("fileName") String filename) {
        return Uni.createFrom().item(toReader(filename));
    }

    @GET
    @Path("/completionStageReader/{fileName}")
    public CompletionStage<Reader> completionStageReader(@PathParam("fileName") String filename) {
        return CompletableFuture.completedStage(toReader(filename));
    }

    @GET
    @Path("/completedFutureReader/{fileName}")
    public CompletableFuture<Reader> completedFutureReader(@PathParam("fileName") String filename) {
        return CompletableFuture.completedFuture(toReader(filename));
    }

    private Reader toReader(String filename) {
        try {
            String f = URLDecoder.decode(filename, "UTF-8");
            return Files.newBufferedReader(Paths.get(f));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
