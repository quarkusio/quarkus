package io.quarkus.tls.cli.letsencrypt;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;

class AcmeClientTest {

    private final AtomicReference<String> requestedPath = new AtomicReference<>();
    private Vertx vertx;
    private HttpServer server;

    @BeforeEach
    void startServer() throws Exception {
        vertx = Vertx.vertx();
        server = vertx.createHttpServer()
                .requestHandler(req -> {
                    requestedPath.set(req.path());
                    req.response().setStatusCode(204).end();
                })
                .listen(0)
                .toCompletionStage().toCompletableFuture().get();
    }

    @AfterEach
    void stopServer() throws Exception {
        vertx.close().toCompletionStage().toCompletableFuture().get();
    }

    @Test
    void testDefaultRootPath() {
        assertChallengePath("", "/q/lets-encrypt/challenge");
    }

    @Test
    void testRootWithTrailingSlash() {
        assertChallengePath("/", "/q/lets-encrypt/challenge");
    }

    @Test
    void testCustomRootPath() {
        assertChallengePath("/admin/lets-encrypt", "/admin/lets-encrypt/challenge");
    }

    @Test
    void testCustomRootPathWithTrailingSlash() {
        assertChallengePath("/admin/lets-encrypt/", "/admin/lets-encrypt/challenge");
    }

    @Test
    void testCertsPath() {
        try (AcmeClient client = newClient("/admin/lets-encrypt")) {
            client.certificateChainAndKeyAreReady();
        }
        Assertions.assertEquals("/admin/lets-encrypt/certs", requestedPath.get());
    }

    private void assertChallengePath(String managementPath, String expectedPath) {
        try (AcmeClient client = newClient(managementPath)) {
            Assertions.assertTrue(client.checkReadiness());
        }
        Assertions.assertEquals(expectedPath, requestedPath.get());
    }

    private AcmeClient newClient(String managementPath) {
        return new AcmeClient("http://localhost:" + server.actualPort() + managementPath, null, null, null, false);
    }
}
