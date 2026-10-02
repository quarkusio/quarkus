package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Random;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.common.annotation.Blocking;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ClientDisconnectResponseLeakTest {

    private static final int BODY_SIZE = 1024 * 1024;
    private static final int UPSTREAM_DELAY_MS = 1000;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(UpstreamResource.class, UpstreamClient.class, ProxyResource.class))
            .overrideRuntimeConfigKey("quarkus.rest-client.upstream.url",
                    "http://localhost:${quarkus.http.test-port:8081}")
            .overrideRuntimeConfigKey("quarkus.rest-client.upstream.connect-timeout", "2000")
            .overrideRuntimeConfigKey("quarkus.rest-client.upstream.read-timeout", "10000")
            .overrideConfigKey("quarkus.rest-client.upstream.connection-pool-size", "2")
            .overrideConfigKey("quarkus.rest-client.upstream.connection-ttl", "30000");

    @TestHTTPResource
    URI uri;

    @Test
    @Order(1)
    @Timeout(30)
    void passThrough_connectionReleasedAfterClientDisconnect() throws Exception {
        verifyNoLeak("/proxy/pass-through", "/proxy/pass-through");
    }

    @Test
    @Order(2)
    @Timeout(30)
    void direct_connectionReleasedAfterClientDisconnect() throws Exception {
        verifyNoLeak("/proxy/direct", "/proxy/pass-through");
    }

    @Test
    @Order(3)
    @Timeout(30)
    void buffered_connectionReleasedAfterClientDisconnect() throws Exception {
        verifyNoLeak("/proxy/buffered", "/proxy/buffered");
    }

    private void verifyNoLeak(String disconnectPath, String verifyPath) throws Exception {
        for (int i = 0; i < 2; i++) {
            disconnectEarly(disconnectPath);
        }

        // Poll until the pool recovers and the verification request succeeds
        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    try (Socket socket = new Socket(uri.getHost(), uri.getPort())) {
                        socket.setSoTimeout(10_000);
                        sendGetRequest(socket, verifyPath);
                        String responseLine = readStatusLine(socket);
                        assertThat(responseLine)
                                .as("REST Client connection pool should not be exhausted after client disconnects (%s)",
                                        disconnectPath)
                                .contains("200");
                    }
                });
    }

    private void disconnectEarly(String path) throws Exception {
        Socket socket = new Socket(uri.getHost(), uri.getPort());
        sendGetRequest(socket, path);
        // Wait for the server to accept the request and start the blocking
        // REST Client call, then disconnect before the upstream responds.
        // 500ms is well within the 1000ms upstream delay.
        Thread.sleep(500);
        socket.close();
    }

    private void sendGetRequest(Socket socket, String path) throws IOException {
        OutputStream out = socket.getOutputStream();
        String request = "GET " + path + " HTTP/1.1\r\n"
                + "Host: " + uri.getHost() + ":" + uri.getPort() + "\r\n"
                + "Connection: close\r\n"
                + "\r\n";
        out.write(request.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private String readStatusLine(Socket socket) throws IOException {
        InputStream in = socket.getInputStream();
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            sb.append((char) c);
            if (c == '\n') {
                break;
            }
        }
        return sb.toString();
    }

    @Path("/upstream")
    public static class UpstreamResource {

        private static final byte[] BODY;

        static {
            BODY = new byte[BODY_SIZE];
            new Random(42).nextBytes(BODY);
        }

        @GET
        @Path("items")
        public byte[] items() throws InterruptedException {
            Thread.sleep(UPSTREAM_DELAY_MS);
            return BODY;
        }
    }

    @Path("/upstream")
    @RegisterRestClient(configKey = "upstream")
    public interface UpstreamClient {

        @GET
        @Path("items")
        Response items();
    }

    @Path("/proxy")
    public static class ProxyResource {

        private final UpstreamClient client;

        public ProxyResource(@RestClient UpstreamClient client) {
            this.client = client;
        }

        @GET
        @Path("pass-through")
        @Blocking
        public Response passThrough() {
            return Response.fromResponse(client.items()).build();
        }

        @GET
        @Path("direct")
        @Blocking
        public Response direct() {
            return client.items();
        }

        @GET
        @Path("buffered")
        @Blocking
        public Response buffered() {
            Response r = client.items();
            r.bufferEntity();
            return Response.fromResponse(r).build();
        }
    }
}
