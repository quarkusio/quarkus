package io.quarkus.oidc.client.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.client.registration.runtime.OidcClientRegistrationException;
import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;

/**
 * Same call as an application that registers several clients on demand,
 * for example {@code CustomTenantConfigResolver} in the oidc-client-registration
 * integration test. The registration endpoint is a stand-in for the provider.
 */
class RegisterClientsTest {

    private static final Vertx VERTX = Vertx.vertx();
    private static final HttpServer SERVER = VERTX.createHttpServer();
    private static final int PORT = startServer();

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withConfiguration("""
                    quarkus.keycloak.devservices.enabled=false
                    quarkus.oidc-client-registration.register-early=false
                    quarkus.oidc-client-registration.registration-path=http://127.0.0.1:%d/register
                    """.formatted(PORT));

    @Inject
    ClientRegistrationStartup startup;

    @AfterAll
    static void stopServer() {
        SERVER.close().toCompletionStage().toCompletableFuture().join();
        VERTX.close().toCompletionStage().toCompletableFuture().join();
    }

    @Test
    void bothAcceptedClientsAreReturned() throws TimeoutException {
        List<RegisteredClient> registered = startup.registerClients(List.of(
                metadata("First Client", "http://localhost:8081/first"),
                metadata("Second Client", "http://localhost:8081/second")));

        assertEquals(Set.of("First Client", "Second Client"),
                registered.stream().map(client -> client.metadata().getClientName()).collect(Collectors.toSet()));
    }

    @Test
    void rejectedClientFailsTheRegistration() {
        Throwable thrown = assertThrows(Throwable.class, () -> startup.registerClients(List.of(
                metadata("Accepted Client", "http://localhost:8081/accepted"),
                metadata("Rejected Client", "http://localhost:8081/rejected"))));

        assertTrue(rejectedByTheProvider(thrown), thrown.toString());
    }

    @Test
    void emptyListCompletesWithoutWaiting() throws TimeoutException {
        assertEquals(List.of(), startup.registerClients(List.of()));
    }

    private static ClientMetadata metadata(String clientName, String redirectUri) {
        return ClientMetadata.builder().clientName(clientName).redirectUri(redirectUri).build();
    }

    private static boolean rejectedByTheProvider(Throwable thrown) {
        for (Throwable current = thrown; current != null; current = current.getCause()) {
            if (current instanceof OidcClientRegistrationException
                    && current.getMessage() != null
                    && current.getMessage().contains("invalid_client_metadata")) {
                return true;
            }
        }
        return false;
    }

    private static int startServer() {
        HttpServer started = SERVER.requestHandler(request -> request.bodyHandler(body -> {
            JsonObject incoming = new JsonObject(body);
            if ("Rejected Client".equals(incoming.getString("client_name"))) {
                request.response().setStatusCode(400)
                        .putHeader("Content-Type", "application/json")
                        .end("{\"error\":\"invalid_client_metadata\",\"error_description\":\"redirect_uri is not allowed\"}");
                return;
            }
            String clientId = incoming.getString("client_name").toLowerCase().replace(' ', '-');
            JsonObject response = incoming.copy();
            response.put("client_id", clientId);
            response.put("client_secret", "secret");
            response.put("registration_client_uri", "http://127.0.0.1:" + PORT + "/register/" + clientId);
            response.put("registration_access_token", "token-" + clientId);
            request.response().setStatusCode(201)
                    .putHeader("Content-Type", "application/json")
                    .end(response.encode());
        })).listen(0).toCompletionStage().toCompletableFuture().join();
        return started.actualPort();
    }

    /**
     * Application code that registers whatever metadata the caller supplies.
     */
    @Singleton
    public static class ClientRegistrationStartup {

        private final OidcClientRegistration clientReg;

        public ClientRegistrationStartup(OidcClientRegistration clientReg) {
            this.clientReg = clientReg;
        }

        public List<RegisteredClient> registerClients(List<ClientMetadata> clients) throws TimeoutException {
            return clientReg.registerClients(clients)
                    .collect().asList()
                    .await().atMost(Duration.ofSeconds(10));
        }
    }
}
