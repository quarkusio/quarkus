package io.quarkus.it.amazon.lambda.rest.resteasy.reactive;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.vertx.http.Compressed;
import io.quarkus.vertx.http.Uncompressed;

@Path("/compression")
public class CompressionResource {

    public static final String MESSAGE = "Hello compression!";

    @GET
    @Path("compressed")
    @Compressed
    @Produces(MediaType.TEXT_PLAIN)
    public String compressed() {
        return MESSAGE;
    }

    @GET
    @Path("uncompressed")
    @Uncompressed
    @Produces(MediaType.TEXT_PLAIN)
    public String uncompressed() {
        return MESSAGE;
    }
}
