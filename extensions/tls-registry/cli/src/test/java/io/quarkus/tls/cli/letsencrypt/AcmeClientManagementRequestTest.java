package io.quarkus.tls.cli.letsencrypt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        AcmeClient client = new AcmeClient(managementUrl, USER, PASSWORD, "acme", false);

        assertTrue(client.checkReadiness());
        client.certificateChainAndKeyAreReady();

        assertEquals("acme", keys.get("GET /q/lets-encrypt/challenge"));
        assertEquals(BASIC, authorizations.get("GET /q/lets-encrypt/challenge"));
        assertEquals("acme", keys.get("POST /q/lets-encrypt/certs"));
        assertEquals(BASIC, authorizations.get("POST /q/lets-encrypt/certs"));
    }

    @Test
    void defaultTlsConfigurationWithBasicAuthentication() {
        AcmeClient client = new AcmeClient(managementUrl, USER, PASSWORD, null, false);

        assertTrue(client.checkReadiness());

        assertNull(keys.get("GET /q/lets-encrypt/challenge"));
        assertEquals(BASIC, authorizations.get("GET /q/lets-encrypt/challenge"));
    }

    @Test
    void namedTlsConfigurationWithoutCredentials() {
        AcmeClient client = new AcmeClient(managementUrl, null, null, "acme", false);

        assertFalse(client.checkReadiness());

        assertEquals("acme", keys.get("GET /q/lets-encrypt/challenge"));
        assertNull(authorizations.get("GET /q/lets-encrypt/challenge"));
    }
}
