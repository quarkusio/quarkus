package io.quarkus.it.gcp.functions.http;

import static io.quarkus.vertx.web.Route.HttpMethod.GET;

import io.quarkus.vertx.http.Compressed;
import io.quarkus.vertx.http.Uncompressed;
import io.quarkus.vertx.web.Route;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class CompressionRoutes {

    public static final String MESSAGE = "Hello compression!";
    public static final int CHUNKS = 16;

    @Compressed
    @Route(path = "/compression/compressed", methods = GET)
    void compressed(RoutingContext context) {
        context.response().putHeader("Content-Type", "text/plain").end(MESSAGE);
    }

    @Uncompressed
    @Route(path = "/compression/uncompressed", methods = GET)
    void uncompressed(RoutingContext context) {
        context.response().putHeader("Content-Type", "text/plain").end(MESSAGE);
    }

    @Compressed
    @Route(path = "/compression/large", methods = GET)
    void large(RoutingContext context) {
        context.response().putHeader("Content-Type", "text/plain").end(body());
    }

    @Compressed
    @Route(path = "/compression/chunked", methods = GET)
    void chunked(RoutingContext context) {
        HttpServerResponse response = context.response().setChunked(true).putHeader("Content-Type", "text/plain");
        for (int i = 0; i < CHUNKS; i++) {
            response.write(chunk(i));
        }
        response.end();
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
