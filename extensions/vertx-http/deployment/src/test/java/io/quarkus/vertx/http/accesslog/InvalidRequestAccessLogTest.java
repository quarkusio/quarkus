package io.quarkus.vertx.http.accesslog;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.ext.web.Router;

public class InvalidRequestAccessLogTest {

    private static final String APP_PROPS = """
            quarkus.http.limits.max-header-size=512
            quarkus.http.limits.max-initial-line-length=128
            quarkus.http.access-log.enabled=true
            quarkus.http.access-log.pattern=common
            """;

    @RegisterExtension
    static final QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Routes.class)
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties"))
            .setLogRecordPredicate(logRecord -> logRecord.getLevel().equals(Level.INFO)
                    && logRecord.getLoggerName().equals("io.quarkus.http.access-log"))
            .assertLogRecords(InvalidRequestAccessLogTest::assertInvalidRequestsLogged);

    @TestHTTPResource
    URL url;

    @Test
    public void testOversizedRequestLineIsLogged() throws Exception {
        sendOversizedRequestLine();
    }

    @Test
    public void testOversizedHeaderIsLogged() throws Exception {
        sendOversizedHeader();
    }

    // Common Log Format: %h %l %u %t "%r" %s %b
    // e.g. 127.0.0.1 - - [23/Jul/2026:12:00:00 +0000] "GET /hello HTTP/1.1" 431 -
    // %h host, then literal "- -" (%l %u), then the CLF timestamp, quoted request line, status and bytes ("-" as
    // no body has been written when the entry is emitted).
    private static final String CLF_HOST = "\\S+";
    private static final String CLF_TIMESTAMP = "\\[\\d{2}/[A-Za-z]{3}/\\d{4}:\\d{2}:\\d{2}:\\d{2} [+-]\\d{4}\\]";

    // The oversized request line cannot be parsed reliably, so only its status/format is asserted.
    private static final Pattern COMMON_414 = Pattern
            .compile("^" + CLF_HOST + " - - " + CLF_TIMESTAMP + " \"[^\"]*\" 414 -$");
    // The oversized header leaves a valid request line, so it is asserted verbatim.
    private static final Pattern COMMON_431 = Pattern
            .compile("^" + CLF_HOST + " - - " + CLF_TIMESTAMP + " \"GET /hello HTTP/1\\.1\" 431 -$");

    private static void assertInvalidRequestsLogged(List<LogRecord> records) {
        List<String> messages = records.stream().map(LogRecord::getMessage).collect(Collectors.toList());
        Assertions.assertTrue(messages.stream().anyMatch(message -> COMMON_414.matcher(message).matches()),
                "Expected a 'common' access log entry for 414, got: " + messages);
        Assertions.assertTrue(messages.stream().anyMatch(message -> COMMON_431.matcher(message).matches()),
                "Expected a 'common' access log entry for 431, got: " + messages);
    }

    private void sendOversizedRequestLine() throws Exception {
        try (Socket socket = new Socket(url.getHost(), url.getPort())) {
            String longPath = "/" + "a".repeat(200);
            String request = "GET " + longPath + " HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n";
            socket.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));
            readResponse(socket);
        }
    }

    private void sendOversizedHeader() throws Exception {
        try (Socket socket = new Socket(url.getHost(), url.getPort())) {
            String largeHeaderValue = "x".repeat(600);
            String request = "GET /hello HTTP/1.1\r\nHost: localhost\r\nX-Large: " + largeHeaderValue
                    + "\r\nConnection: close\r\n\r\n";
            socket.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));
            readResponse(socket);
        }
    }

    private static void readResponse(Socket socket) throws IOException {
        byte[] data = new byte[4096];
        try {
            socket.getInputStream().read(data);
        } catch (IOException e) {
            // connection reset is acceptable for invalid requests
        }
    }

    @ApplicationScoped
    static class Routes {
        public void register(@Observes Router router) {
            router.route("/hello").handler(rc -> rc.response().end("hello"));
        }
    }
}
