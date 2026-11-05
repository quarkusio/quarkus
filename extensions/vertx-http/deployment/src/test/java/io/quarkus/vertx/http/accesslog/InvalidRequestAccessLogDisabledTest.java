package io.quarkus.vertx.http.accesslog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.ext.web.Router;

public class InvalidRequestAccessLogDisabledTest {

    private static final String APP_PROPS = """
            quarkus.http.limits.max-header-size=512
            quarkus.http.limits.max-initial-line-length=128
            quarkus.http.access-log.enabled=false
            """;

    @RegisterExtension
    static final QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Routes.class)
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties"))
            .setLogRecordPredicate(logRecord -> logRecord.getLevel().equals(Level.INFO)
                    && logRecord.getLoggerName().equals("io.quarkus.http.access-log"))
            .assertLogRecords(InvalidRequestAccessLogDisabledTest::assertNoAccessLogs);

    @TestHTTPResource
    URL url;

    @Test
    public void testOversizedRequestLineWhenAccessLogDisabled() throws Exception {
        String response = sendOversizedRequestLine();
        Assertions.assertTrue(response.contains(" 414 ") || response.isEmpty(),
                "Expected 414 response or connection close, got: " + response);
    }

    @Test
    public void testOversizedHeaderWhenAccessLogDisabled() throws Exception {
        String response = sendOversizedHeader("/hello");
        Assertions.assertTrue(response.contains(" 431 ") || response.isEmpty(),
                "Expected 431 response or connection close, got: " + response);
    }

    private static void assertNoAccessLogs(List<LogRecord> records) {
        Assertions.assertTrue(records.isEmpty(),
                "Expected no access log records when access-log is disabled, but got: " + records);
    }

    private String sendOversizedRequestLine() throws Exception {
        try (Socket socket = new Socket(url.getHost(), url.getPort())) {
            String longPath = "/" + "a".repeat(200);
            String request = "GET " + longPath + " HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n";
            socket.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));
            return readResponse(socket);
        }
    }

    private String sendOversizedHeader(String path) throws Exception {
        try (Socket socket = new Socket(url.getHost(), url.getPort())) {
            String largeHeaderValue = "x".repeat(600);
            String request = "GET " + path + " HTTP/1.1\r\nHost: localhost\r\nX-Large: " + largeHeaderValue
                    + "\r\nConnection: close\r\n\r\n";
            socket.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));
            return readResponse(socket);
        }
    }

    private static String readResponse(Socket socket) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        try {
            int read = socket.getInputStream().read(data);
            if (read > 0) {
                out.write(data, 0, read);
            }
        } catch (IOException e) {
            // connection reset is acceptable for invalid requests
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    @ApplicationScoped
    static class Routes {
        public void register(@Observes Router router) {
            router.route("/hello").handler(rc -> rc.response().end("hello"));
        }
    }
}
