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
 * Tests that idle timeout is applied to HTTP connections.
 */
public class IdleTimeoutTest {

    private static final String APP_PROPS = "quarkus.http.idle-timeout=2S\n";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties")
                    .addClasses(Routes.class));

    @TestHTTPResource
    URL url;

    @Test
    public void testConnectionClosedAfterIdleTimeout() throws Exception {
        try (Socket socket = new Socket(url.getHost(), url.getPort())) {
            // Send a valid request
            socket.getOutputStream().write("GET /hello HTTP/1.1\r\nHost: localhost\r\n\r\n"
                    .getBytes(StandardCharsets.UTF_8));
            byte[] data = new byte[4096];
            StringBuilder sb = new StringBuilder();
            while (!sb.toString().contains("hello")) {
                int read = socket.getInputStream().read(data);
                if (read == -1) {
                    Assertions.fail("Connection closed before response received");
                }
                sb.append(new String(data, 0, read, StandardCharsets.US_ASCII));
            }
            Assertions.assertTrue(sb.toString().contains("HTTP/1.1 200"));

            // Now wait for idle timeout (2 seconds) + buffer
            Thread.sleep(3000);

            // Connection should be closed by the server
            try {
                socket.getOutputStream().write("GET /hello HTTP/1.1\r\nHost: localhost\r\n\r\n"
                        .getBytes(StandardCharsets.UTF_8));
                int read = socket.getInputStream().read(data);
                // Either we get -1 (EOF) or an IOException
                if (read != -1) {
                    // Some OS may still deliver data before reporting the reset
                    // Try reading again
                    read = socket.getInputStream().read(data);
                    Assertions.assertEquals(-1, read, "Expected connection to be closed after idle timeout");
                }
            } catch (IOException e) {
                // Expected - connection reset by peer
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
