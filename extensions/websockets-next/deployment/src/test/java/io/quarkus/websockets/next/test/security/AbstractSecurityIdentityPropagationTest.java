package io.quarkus.websockets.next.test.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnTextMessage;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;

public abstract class AbstractSecurityIdentityPropagationTest {

    @Inject
    Vertx vertx;

    @TestHTTPResource("echo-multi-produce")
    URI echoMultiProduce;

    protected static QuarkusUnitTest getQuarkusUnitTest(String applicationProperties, Class<?>... additionalClasses) {
        return new QuarkusUnitTest()
                .withApplicationRoot(root -> root
                        .addClasses(EchoMultiProduce.class, TestIdentityProvider.class, MyService.class,
                                TestIdentityController.class)
                        .addClasses(additionalClasses)
                        .addAsResource(new StringAsset(applicationProperties), "application.properties"));
    }

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin")
                .add("user", "user", "user")
                .add("martin", "martin", "martin");
    }

    @Test
    public void testPropagationWithMultiAndDelay() throws Exception {
        WebSocketClient client1 = null;
        WebSocketClient client2 = null;
        WebSocketClient client3 = null;
        try {
            List<String> messages = new CopyOnWriteArrayList<>();
            client1 = testEcho("admin", messages);
            client2 = testEcho("user", messages);
            client3 = testEcho("martin", messages);

            Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> assertTrue(messages.size() > 2));
            Supplier<String> errorMessage = () -> "Expected 3 messages for 3 users: 'admin', 'user' and 'martin'. Found messages were "
                    + messages;
            assertTrue(messages.stream().anyMatch(message -> message.contains("admin hello admin")), errorMessage);
            assertTrue(messages.stream().anyMatch(message -> message.contains("user hello user")), errorMessage);
            assertTrue(messages.stream().anyMatch(message -> message.contains("martin hello martin")), errorMessage);
        } finally {
            if (client1 != null) {
                client1.close().toCompletionStage().toCompletableFuture().get();
            }
            if (client2 != null) {
                client2.close().toCompletionStage().toCompletableFuture().get();
            }
            if (client3 != null) {
                client3.close().toCompletionStage().toCompletableFuture().get();
            }
        }
    }

    private WebSocketClient testEcho(String user, List<String> messages) {
        WebSocketClient client = vertx.createWebSocketClient();
        client
                .connect(basicAuth(user))
                .onComplete(r -> {
                    if (r.succeeded()) {
                        WebSocket ws = r.result();
                        ws.textMessageHandler(messages::add);
                        ws.writeTextMessage("hello");
                    } else {
                        throw new IllegalStateException(r.cause());
                    }
                });
        return client;
    }

    private WebSocketConnectOptions basicAuth(String user) {
        return new WebSocketConnectOptions().addHeader(HttpHeaders.AUTHORIZATION.toString(),
                new UsernamePasswordCredentials(user, user)
                        .applyHttpChallenge(null).toHttpAuthorization())
                .setHost(echoMultiProduce.getHost()).setPort(echoMultiProduce.getPort())
                .setURI(echoMultiProduce.getPath());
    }

    @Authenticated
    @io.quarkus.websockets.next.WebSocket(path = "/echo-multi-produce")
    public static class EchoMultiProduce {

        @Inject
        SecurityIdentity securityIdentity;

        @Inject
        MyService service;

        @OnTextMessage
        Multi<String> echo(String msg) {
            return service.echo(securityIdentity.getPrincipal().getName() + " " + msg);
        }

    }

    @SessionScoped
    public static class MyService {

        @Inject
        SecurityIdentity securityIdentity;

        Multi<String> echo(String msg) {
            return Multi.createFrom().item(msg)
                    .onItem().call(i ->
                    // Delay the emission until the returned uni emits its item
                    Uni.createFrom().nullItem().onItem().delayIt().by(Duration.ofSeconds(1)))
                    .map(m -> m + " " + securityIdentity.getPrincipal().getName());
        }

    }

}
