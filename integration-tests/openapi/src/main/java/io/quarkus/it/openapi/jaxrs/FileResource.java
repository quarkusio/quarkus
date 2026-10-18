package io.quarkus.it.openapi.jaxrs;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
public class FileResource {
    @GET
    @Path("/justFile/{fileName}")
    public File justFile(@PathParam("fileName") String filename) {
        return toFile(filename);
    }

    @POST
    @Path("/justFile")
    public File justFile(File file) {
        return file;
    }

    @GET
    @Path("/restResponseFile/{fileName}")
    public RestResponse<File> restResponseFile(@PathParam("fileName") String filename) {
        return RestResponse.ok(toFile(filename));
    }

    @POST
    @Path("/restResponseFile")
    public RestResponse<File> restResponseFile(File file) {
        return RestResponse.ok(file);
    }

    @GET
    @Path("/optionalFile/{fileName}")
    public Optional<File> optionalFile(@PathParam("fileName") String filename) {
        return Optional.of(toFile(filename));
    }

    @POST
    @Path("/optionalFile")
    public Optional<File> optionalFile(Optional<File> file) {
        return file;
    }

    @GET
    @Path("/uniFile/{fileName}")
    public Uni<File> uniFile(@PathParam("fileName") String filename) {
        return Uni.createFrom().item(toFile(filename));
    }

    @GET
    @Path("/completionStageFile/{fileName}")
    public CompletionStage<File> completionStageFile(@PathParam("fileName") String filename) {
        return CompletableFuture.completedStage(toFile(filename));
    }

    @GET
    @Path("/completedFutureFile/{fileName}")
    public CompletableFuture<File> completedFutureFile(@PathParam("fileName") String filename) {
        return CompletableFuture.completedFuture(toFile(filename));
    }

    private File toFile(String filename) {
        try {
            String f = URLDecoder.decode(filename, "UTF-8");
            return new File(f);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }
}
