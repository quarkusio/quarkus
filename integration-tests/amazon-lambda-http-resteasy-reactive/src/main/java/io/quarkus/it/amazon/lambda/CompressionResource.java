package io.quarkus.it.amazon.lambda;

import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.StreamingOutput;

import io.quarkus.vertx.http.Compressed;
import io.quarkus.vertx.http.Uncompressed;

@Path("/compression")
public class CompressionResource {

    public static final String MESSAGE = "Hello compression!";
    public static final int CHUNKS = 16;

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

    @GET
    @Path("large")
    @Compressed
    @Produces(MediaType.TEXT_PLAIN)
    public String large() {
        return body();
    }

    @GET
    @Path("chunked")
    @Compressed
    @Produces(MediaType.TEXT_PLAIN)
    public StreamingOutput chunked() {
        return output -> {
            for (int i = 0; i < CHUNKS; i++) {
                output.write(chunk(i).getBytes(StandardCharsets.UTF_8));
                output.flush();
            }
        };
    }

    public static String body() {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < CHUNKS; i++) {
            body.append(chunk(i));
        }
        return body.toString();
    }

    private static String chunk(int index) {
        StringBuilder chunk = new StringBuilder();
        for (int line = 0; line < 256; line++) {
            chunk.append("chunk ").append(index).append(", line ").append(line).append('\n');
        }
        return chunk.toString();
    }
}
