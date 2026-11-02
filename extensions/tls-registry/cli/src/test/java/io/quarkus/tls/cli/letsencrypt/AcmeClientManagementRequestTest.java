package io.quarkus.tls.cli.letsencrypt;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;

/**
 * Checks the TLS configuration name and the credentials that {@link AcmeClient} sends to the management endpoint.
 */
public class AcmeClientManagementRequestTest {

    private static final String USER = "admin";
    private static final String PASSWORD = "secret";
    private static final String BASIC = "Basic "
            + Base64.getEncoder().encodeToString((USER + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));

    private static Vertx vertx;
    private static HttpServer server;
    private static String managementUrl;

    private static final Map<String, String> keys = new ConcurrentHashMap<>();
    private static final Map<String, String> authorizations = new ConcurrentHashMap<>();

    @BeforeAll
    static void startServer() {
        vertx = Vertx.vertx();
        server = vertx.createHttpServer()
                .requestHandler(request -> {
                    String call = request.method() + " " + request.path();
                    String key = request.getParam("key");
                    if (key != null) {
                        keys.put(call, key);
                    }
                    String authorization = request.getHeader("Authorization");
                    if (authorization != null) {
                        authorizations.put(call, authorization);
                    }
                    request.response().setStatusCode(BASIC.equals(authorization) ? 204 : 401).end();
                })
                .listen(0)
                .await();
        managementUrl = "http://localhost:" + server.actualPort();
    }

    @AfterAll
    static void stopServer() {
        vertx.close().await();
    }

    @BeforeEach
    void clear() {
        keys.clear();
        authorizations.clear();
    }

    @Test
    void namedTlsConfigurationWithBasicAuthentication() {
        try (AcmeClient client = new AcmeClient(managementUrl, USER, PASSWORD, "acme", false)) {
            assertThat(client.checkReadiness()).isTrue();
            client.certificateChainAndKeyAreReady();
        }

        assertThat(keys)
                .containsEntry("GET /q/lets-encrypt/challenge", "acme")
                .containsEntry("POST /q/lets-encrypt/certs", "acme");
        assertThat(authorizations)
                .containsEntry("GET /q/lets-encrypt/challenge", BASIC)
                .containsEntry("POST /q/lets-encrypt/certs", BASIC);
    }

    @Test
    void defaultTlsConfigurationWithBasicAuthentication() {
        try (AcmeClient client = new AcmeClient(managementUrl, USER, PASSWORD, null, false)) {
            assertThat(client.checkReadiness()).isTrue();
        }

        assertThat(keys).isEmpty();
        assertThat(authorizations).containsEntry("GET /q/lets-encrypt/challenge", BASIC);
    }

    @Test
    void namedTlsConfigurationWithoutCredentials() {
        try (AcmeClient client = new AcmeClient(managementUrl, null, null, "acme", false)) {
            assertThat(client.checkReadiness()).isFalse();
        }

        assertThat(keys).containsEntry("GET /q/lets-encrypt/challenge", "acme");
        assertThat(authorizations).isEmpty();
    }

    @Test
    void defaultTlsConfigurationWithoutCredentials() {
        try (AcmeClient client = new AcmeClient(managementUrl, null, null, null, false)) {
            assertThat(client.checkReadiness()).isFalse();
        }

        assertThat(keys).isEmpty();
        assertThat(authorizations).isEmpty();
    }
}
