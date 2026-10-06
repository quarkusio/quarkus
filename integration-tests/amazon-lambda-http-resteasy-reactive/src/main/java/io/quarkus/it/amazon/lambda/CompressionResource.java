package io.quarkus.it.amazon.lambda;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.vertx.http.Compressed;

@Path("/compressed")
public class CompressionResource {

    public static final String MESSAGE = "Hello compression!";

    @GET
    @Compressed
    @Produces(MediaType.TEXT_PLAIN)
    public String compressed() {
        return MESSAGE;
    }
}
