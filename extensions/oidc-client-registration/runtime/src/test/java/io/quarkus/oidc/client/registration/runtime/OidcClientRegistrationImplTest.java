package io.quarkus.oidc.client.registration.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.oidc.client.registration.ClientMetadata;
import io.quarkus.oidc.client.registration.OidcClientRegistrationConfig;
import io.quarkus.oidc.client.registration.RegisteredClient;
import io.quarkus.oidc.common.runtime.OidcTlsSupport;
import io.quarkus.oidc.common.runtime.OidcWebClient;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpServer;

/**
 * Plain unit test (no Quarkus application) for {@link OidcClientRegistrationImpl#registerClients(List)}.
 */
public class OidcClientRegistrationImplTest {

    private Vertx vertx;
    private HttpServer server;
    private OidcWebClient webClient;

    @BeforeEach
    public void setUp() {
        vertx = Vertx.vertx();
        server = vertx.createHttpServer()
                .requestHandler(request -> request.body().subscribe().with(body -> {
                    if ("a".equals(body.toJsonObject().getString("client_name"))) {
                        request.response().setStatusCode(400).endAndForget("{}");
                    } else {
                        // Other clients are registered after "a" has failed
                        vertx.setTimer(200, id -> request.response().setStatusCode(201).endAndForget(body.toString()));
                    }
                }))
                .listen(0)
                .await().atMost(Duration.ofSeconds(5));
        webClient = OidcWebClient.create(OidcClientRegistrationConfig.builder().build(), OidcTlsSupport.empty(), vertx,
                null, "test");
    }

    @AfterEach
    public void tearDown() {
        webClient.close();
        vertx.closeAndAwait();
    }

    @Test
    public void testRegisterNoClientsCompletes() {
        List<RegisteredClient> clients = registration().registerClients(List.of())
                .collect().asList()
                .await().atMost(Duration.ofSeconds(5));
        assertTrue(clients.isEmpty());
    }

    @Test
    public void testRegisterClientsPropagatesFailure() {
        List<ClientMetadata> metadata = List.of(new ClientMetadata("{\"client_name\":\"a\"}"),
                new ClientMetadata("{\"client_name\":\"b\"}"));
        Throwable failure = assertThrows(Throwable.class, () -> registration().registerClients(metadata)
                .collect().asList()
                .await().atMost(Duration.ofSeconds(5)));
        assertInstanceOf(OidcClientRegistrationException.class, failure);
    }

    @Test
    public void testRegisterClientsDeliversSuccessfulRegistrationsDespiteFailure() {
        List<ClientMetadata> metadata = List.of(new ClientMetadata("{\"client_name\":\"a\"}"),
                new ClientMetadata("{\"client_name\":\"b\"}"));
        List<String> registered = new CopyOnWriteArrayList<>();
        Throwable failure = assertThrows(Throwable.class, () -> registration().registerClients(metadata)
                .onItem().invoke(client -> registered.add(client.metadata().getClientName()))
                .collect().asList()
                .await().atMost(Duration.ofSeconds(5)));
        assertInstanceOf(OidcClientRegistrationException.class, failure);
        assertEquals(List.of("b"), registered);
    }

    private OidcClientRegistrationImpl registration() {
        return new OidcClientRegistrationImpl(webClient, 0, "http://localhost:" + server.actualPort() + "/register",
                OidcClientRegistrationConfig.builder().build(), null, Map.of(), Map.of());
    }
}
