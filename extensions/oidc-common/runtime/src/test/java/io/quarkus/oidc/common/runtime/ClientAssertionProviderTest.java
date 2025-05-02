package io.quarkus.oidc.common.runtime;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import io.smallrye.jwt.build.Jwt;
import io.vertx.core.Vertx;

public class ClientAssertionProviderTest {

    @Test
    public void testJwtBearerTokenRefresh() {
        // if this test ever gets flaky, we will need longer expires in and waiting between storing / refresh
        Vertx vertx = Vertx.vertx();
        Path jwtBearerTokenPath = Path.of("target").resolve("jwt-bearer-token.json");
        String jwtBearerToken = createJwtBearerToken();
        storeNewJwtBearerToken(jwtBearerTokenPath, jwtBearerToken);
        try (var clientAssertionProvider = new ClientAssertionProvider(vertx, jwtBearerTokenPath)) {
            // assert first token is loaded
            assertEquals(jwtBearerToken, clientAssertionProvider.getClientAssertion());

            // create a new token
            String secondJwtBearerToken = createJwtBearerToken();
            storeNewJwtBearerToken(jwtBearerTokenPath, secondJwtBearerToken);

            Awaitility.await().atMost(Duration.ofSeconds(10))
                    .untilAsserted(() -> assertEquals(secondJwtBearerToken, clientAssertionProvider.getClientAssertion()));
        } finally {
            vertx.close().toCompletionStage().toCompletableFuture().join();
        }
    }

    @Test
    public void EmptyBearerTokenFileShouldReturnNullClientAssertion() {
        Vertx vertx = Vertx.vertx();
        Path emptyTokenPath = Path.of("target").resolve("empty-jwt-bearer-token.json");

        storeNewJwtBearerToken(emptyTokenPath, "");
        try (var clientAssertionProvider = new ClientAssertionProvider(vertx, emptyTokenPath)) {
            assertNull(clientAssertionProvider.getClientAssertion());

            String validToken = createJwtBearerToken();
            storeNewJwtBearerToken(emptyTokenPath, validToken);

            Awaitility.await().atMost(Duration.ofSeconds(10))
                    .untilAsserted(() -> assertEquals(validToken, clientAssertionProvider.getClientAssertion()));
        } finally {
            vertx.close().toCompletionStage().toCompletableFuture().join();
        }
    }

    private static void storeNewJwtBearerToken(Path jwtBearerTokenPath, String jwtBearerToken) {
        try {
            Files.writeString(jwtBearerTokenPath, jwtBearerToken, TRUNCATE_EXISTING, CREATE, WRITE);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write JWT bearer token", e);
        }
    }

    private static String createJwtBearerToken() {
        return Jwt.preferredUserName("Arnold")
                .issuer("https://server.example.com")
                .audience("https://service.example.com")
                .expiresIn(Duration.ofSeconds(4))
                .signWithSecret("43".repeat(20));
    }

}
