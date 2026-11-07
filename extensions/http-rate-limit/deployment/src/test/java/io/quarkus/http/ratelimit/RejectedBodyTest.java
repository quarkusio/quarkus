package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

/**
 * The body of a refused request is drained so that the connection can serve the next request, but no further than
 * {@code quarkus.http.limits.max-body-size}, the most a served request may send: past it, the connection is closed, so
 * that a refused client cannot hold a connection open with a huge or endless body.
 */
public class RejectedBodyTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = TestHttp.ipRule(new QuarkusExtensionTest(), "api", "/api/*", 1, "1h")
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestHttp.Resource.class))
            .overrideConfigKey("quarkus.http.limits.max-body-size", "1K");

    @TestHTTPResource
    URL url;

    @Test
    void refusedRequestsAreDrainedWithinTheMaxBodySize() throws IOException {
        try (Socket socket = socket()) {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            // the first request spends the permit; the second is refused, and its body is drained
            for (int i = 0; i < 2; i++) {
                out.write(request("Content-Length: 100\r\n").getBytes(StandardCharsets.US_ASCII));
                out.write(new byte[100]);
                out.flush();
                String head = readResponseHead(in);
                assertThat(head).startsWith(i == 0 ? "HTTP/1.1 200" : "HTTP/1.1 429");
                skipBody(in, head);
            }
            // the connection still serves requests
            out.write(request("Content-Length: 0\r\n").getBytes(StandardCharsets.US_ASCII));
            out.flush();
            assertThat(readResponseHead(in)).startsWith("HTTP/1.1 429");
        }
    }

    @Test
    void declaredBodyOverTheMaxBodySizeClosesTheConnection() throws IOException {
        spendPermit();
        try (Socket socket = socket()) {
            socket.getOutputStream().write(request("Content-Length: 1000000000\r\n")
                    .getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
            InputStream in = socket.getInputStream();
            String head = readResponseHead(in);
            assertThat(head).startsWith("HTTP/1.1 429").containsIgnoringCase("connection: close");
            assertClosed(in);
        }
    }

    @Test
    void chunkedBodyOverTheMaxBodySizeClosesTheConnection() throws IOException {
        spendPermit();
        try (Socket socket = socket()) {
            OutputStream out = socket.getOutputStream();
            out.write(request("Transfer-Encoding: chunked\r\n").getBytes(StandardCharsets.US_ASCII));
            out.flush();
            InputStream in = socket.getInputStream();
            String head = readResponseHead(in);
            assertThat(head).startsWith("HTTP/1.1 429");
            try {
                // 32K, far over the 1K limit, of a body that never ends
                for (int i = 0; i < 64; i++) {
                    out.write(("200\r\n" + "x".repeat(512) + "\r\n").getBytes(StandardCharsets.US_ASCII));
                    out.flush();
                }
            } catch (IOException closed) {
                // the server may close before every chunk is written
            }
            skipBody(in, head);
            assertClosed(in);
        }
    }

    /**
     * The HTTP decoder refuses a malformed {@code Content-Length} before any route runs, so the limiter only ever sees
     * a valid one.
     */
    @Test
    void malformedContentLengthIsRefusedBeforeTheLimiter() throws IOException {
        spendPermit();
        try (Socket socket = socket()) {
            socket.getOutputStream().write(request("Content-Length: 12x\r\n").getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
            assertThat(readResponseHead(socket.getInputStream())).startsWith("HTTP/1.1 400");
        }
    }

    private void spendPermit() throws IOException {
        try (Socket socket = socket()) {
            socket.getOutputStream().write(request("Content-Length: 0\r\n").getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
            readResponseHead(socket.getInputStream());
        }
    }

    private Socket socket() throws IOException {
        Socket socket = new Socket(url.getHost(), url.getPort());
        socket.setSoTimeout(5_000);
        return socket;
    }

    private String request(String bodyHeader) {
        return "POST /api/x HTTP/1.1\r\nHost: " + url.getHost() + "\r\n" + bodyHeader + "\r\n";
    }

    private static String readResponseHead(InputStream in) throws IOException {
        StringBuilder head = new StringBuilder();
        while (!head.toString().endsWith("\r\n\r\n")) {
            int b = in.read();
            if (b < 0) {
                throw new IOException("closed before the response head: " + head);
            }
            head.append((char) b);
        }
        return head.toString();
    }

    private static void skipBody(InputStream in, String head) throws IOException {
        for (String line : head.split("\r\n")) {
            if (line.toLowerCase(java.util.Locale.ROOT).startsWith("content-length:")) {
                in.readNBytes(Integer.parseInt(line.substring("content-length:".length()).trim()));
            }
        }
    }

    private static void assertClosed(InputStream in) throws IOException {
        // the server closes: read returns -1, or the connection is reset; a timeout means it is still open
        try {
            while (true) {
                if (in.read() < 0) {
                    return;
                }
            }
        } catch (java.net.SocketTimeoutException e) {
            throw new AssertionError("the connection is still open", e);
        } catch (IOException reset) {
            // closed
        }
    }
}
