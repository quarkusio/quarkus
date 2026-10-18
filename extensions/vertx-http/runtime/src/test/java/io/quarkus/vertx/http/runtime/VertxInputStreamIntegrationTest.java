package io.quarkus.vertx.http.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

class VertxInputStreamIntegrationTest {

    private Vertx vertx;
    private HttpServer server;
    private HttpClient client;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        vertx = Vertx.vertx();
        Router router = Router.router(vertx);

        router.post("/read-body").handler(ctx -> {
            ctx.request().pause();
            vertx.executeBlocking(() -> {
                try {
                    return readFullBody(ctx);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).onComplete(ar -> {
                if (ar.succeeded()) {
                    ctx.response().end(Buffer.buffer(ar.result()));
                } else {
                    ctx.response().setStatusCode(500).end(ar.cause().getMessage());
                }
            });
        });

        router.post("/read-body-sha256").handler(ctx -> {
            ctx.request().pause();
            vertx.executeBlocking(() -> {
                try {
                    return sha256(ctx);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).onComplete(ar -> {
                if (ar.succeeded()) {
                    ctx.response().end(ar.result());
                } else {
                    ctx.response().setStatusCode(500).end(ar.cause().getMessage());
                }
            });
        });

        server = vertx.createHttpServer()
                .requestHandler(router)
                .listen(0)
                .await(10, TimeUnit.SECONDS);
        port = server.actualPort();
        client = vertx.createHttpClient();
    }

    @AfterEach
    void tearDown() throws TimeoutException {
        if (client != null) {
            client.close().await(5, TimeUnit.SECONDS);
        }
        if (server != null) {
            server.close().await(5, TimeUnit.SECONDS);
        }
        if (vertx != null) {
            vertx.close().await(5, TimeUnit.SECONDS);
        }
    }

    private byte[] readFullBody(RoutingContext ctx) throws IOException {
        try (VertxInputStream vis = new VertxInputStream(ctx, 30000)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int read;
            while ((read = vis.read(buf)) != -1) {
                baos.write(buf, 0, read);
            }
            return baos.toByteArray();
        }
    }

    private String sha256(RoutingContext ctx) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (VertxInputStream vis = new VertxInputStream(ctx, 30000)) {
            byte[] buf = new byte[8192];
            int read;
            while ((read = vis.read(buf)) != -1) {
                digest.update(buf, 0, read);
            }
        }
        return hexEncode(digest.digest());
    }

    private static String hexEncode(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private Buffer sendPost(Buffer body, String path) throws TimeoutException {
        return client.request(HttpMethod.POST, port, "localhost", path)
                .compose(req -> req.send(body))
                .compose(resp -> resp.body())
                .await(30, TimeUnit.SECONDS);
    }

    @Test
    void simplePostBody() throws Exception {
        byte[] data = "Hello, World!".getBytes();
        Buffer response = sendPost(Buffer.buffer(data), "/read-body");
        assertThat(response.getBytes()).isEqualTo(data);
    }

    @Test
    void emptyPostBody() throws Exception {
        Buffer response = sendPost(Buffer.buffer(), "/read-body");
        assertThat(response.length()).isEqualTo(0);
    }

    @Test
    void largePostBody() throws Exception {
        byte[] data = new byte[128 * 1024];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 251);
        }
        Buffer response = sendPost(Buffer.buffer(data), "/read-body");
        assertThat(response.getBytes()).isEqualTo(data);
    }

    @Test
    void veryLargePostBody() throws Exception {
        // 5MB body — verifies sustained streaming and backpressure
        int size = 5 * 1024 * 1024;
        byte[] data = new byte[size];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 251);
        }

        String expected = hexEncode(MessageDigest.getInstance("SHA-256").digest(data));

        // Compare SHA-256 instead of transferring 5MB back in the response
        Buffer response = sendPost(Buffer.buffer(data), "/read-body-sha256");
        assertThat(response.toString()).isEqualTo(expected);
    }
}
