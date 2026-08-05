package io.quarkus.vertx.http;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.ext.web.Router;

/**
 * Tests server limits configuration (max-header-size, max-initial-line-length).
 */
public class ServerLimitsTest {

    private static final String APP_PROPS = """
            quarkus.http.limits.max-header-size=512
            quarkus.http.limits.max-initial-line-length=128
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties")
                    .addClasses(Routes.class));

    @TestHTTPResource
    URL url;

    @Test
    public void testNormalRequestSucceeds() throws Exception {
        try (Socket socket = new Socket(url.getHost(), url.getPort())) {
            socket.getOutputStream().write(
                    "GET /hello HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n"
                            .getBytes(StandardCharsets.UTF_8));
            String response = new String(socket.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);
            Assertions.assertTrue(response.contains("HTTP/1.1 200"), "Expected 200 OK, got: " + response);
        }
    }

    @Test
    public void testOversizedHeaderRejected() throws Exception {
        try (Socket socket = new Socket(url.getHost(), url.getPort())) {
            // Send a header value exceeding 512 bytes
            String largeHeaderValue = "x".repeat(600);
            String request = "GET /hello HTTP/1.1\r\nHost: localhost\r\nX-Large: " + largeHeaderValue
                    + "\r\nConnection: close\r\n\r\n";
            socket.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));
            String response = new String(socket.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);
            // Vert.x returns 431 for header too large
            Assertions.assertTrue(response.contains("431") || response.contains("400"),
                    "Expected 431 or 400 for oversized header, got: " + response);
        }
    }

    @Test
    public void testOversizedRequestLineRejected() throws Exception {
        try (Socket socket = new Socket(url.getHost(), url.getPort())) {
            // Create a URI longer than 128 bytes
            String longPath = "/" + "a".repeat(200);
            String request = "GET " + longPath + " HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n";
            socket.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));

            byte[] data = new byte[4096];
            try {
                int read = socket.getInputStream().read(data);
                if (read > 0) {
                    String response = new String(data, 0, read, StandardCharsets.US_ASCII);
                    // Vert.x returns 414 for URI too long
                    Assertions.assertTrue(response.contains("414") || response.contains("400"),
                            "Expected 414 or 400 for oversized request line, got: " + response);
                }
                // If read == -1, the connection was reset which is also acceptable
            } catch (IOException e) {
                // Connection reset is acceptable
            }
        }
    }

    @ApplicationScoped
    static class Routes {
        public void register(@Observes Router router) {
            router.route("/hello").handler(rc -> rc.response().end("hello"));
        }
    }
}
