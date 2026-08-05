package io.quarkus.vertx.http;

import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.deployment.util.IoUtil;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class InvalidUriTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withEmptyApplication();

    @TestHTTPResource
    URI uri;

    @Test
    public void testMissingUri() throws Exception {
        // Send HTTP request with missing URI (malformed - double space between GET and HTTP/1.1)
        // This can cause uri() to return null
        try (Socket s = new Socket(uri.getHost(), uri.getPort())) {
            s.getOutputStream().write("GET  HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n"
                    .getBytes(StandardCharsets.UTF_8));
            String result = new String(IoUtil.readBytes(s.getInputStream()), StandardCharsets.UTF_8);
            // Server returns HTTP/1.0 for malformed request lines (protocol downgrade)
            Assertions.assertTrue(result.contains("HTTP/1.0 400 Bad Request"),
                    "Expected 400 Bad Request but got: " + result);
        }
    }

    @Test
    public void testUnescapedSpace() throws Exception {
        // Send HTTP request with unescaped space in URI path
        // This should trigger URISyntaxException
        try (Socket s = new Socket(uri.getHost(), uri.getPort())) {
            s.getOutputStream().write("GET /foo bar HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n"
                    .getBytes(StandardCharsets.UTF_8));
            String result = new String(IoUtil.readBytes(s.getInputStream()), StandardCharsets.UTF_8);
            // Server returns HTTP/1.0 for malformed request lines (protocol downgrade)
            Assertions.assertTrue(result.contains("HTTP/1.0 400 Bad Request"),
                    "Expected 400 Bad Request for unescaped space in URI but got: " + result);
        }
    }
}
