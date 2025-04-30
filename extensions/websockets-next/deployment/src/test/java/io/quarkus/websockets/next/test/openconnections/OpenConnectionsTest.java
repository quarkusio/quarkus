package io.quarkus.websockets.next.test.openconnections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OpenConnections;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocketConnectOptions;

public class OpenConnectionsTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Endpoint.class, WSClient.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("endpoint")
    URI endUri;

    @Inject
    OpenConnections connections;

    @Test
    void testOpenConnections() throws Exception {
        String headerName = "X-Test";
        String header2 = "foo";
        String header3 = "bar";

        for (WebSocketConnection c : connections) {
            fail("No connection should be found: " + c);
        }

        try (WSClient client1 = WSClient.create(vertx).connect(endUri);
                WSClient client2 = WSClient.create(vertx).connect(new WebSocketConnectOptions().addHeader(headerName, header2),
                        endUri);
                WSClient client3 = WSClient.create(vertx).connect(new WebSocketConnectOptions().addHeader(headerName, header3),
                        endUri)) {

            client1.waitForMessages(1);
            String client1Id = client1.getMessages().get(0).toString();

            client2.waitForMessages(1);
            String client2Id = client2.getMessages().get(0).toString();

            client3.waitForMessages(1);
            String client3Id = client3.getMessages().get(0).toString();

            assertNotNull(connections.findByConnectionId(client1Id).orElse(null));
            Collection<WebSocketConnection> found = connections.stream()
                    .filter(c -> header3.equals(c.handshakeRequest().header(headerName)))
                    .toList();
            assertEquals(1, found.size());
            assertEquals(client3Id, found.iterator().next().id());

            found = connections.listAll();
            assertEquals(3, found.size());
            for (WebSocketConnection c : found) {
                assertTrue(c.id().equals(client1Id) || c.id().equals(client2Id) || c.id().equals(client3Id));
            }

            client2.disconnect();
            assertTrue(Endpoint.CLOSED_LATCH.await(5, TimeUnit.SECONDS));

            assertEquals(2, connections.listAll().size());
            assertNull(connections.stream().filter(c -> c.id().equals(client2Id)).findFirst().orElse(null));

            found = connections.findByEndpointId("end");
            assertEquals(2, found.size());
        }
    }

    @WebSocket(endpointId = "end", path = "/endpoint")
    public static class Endpoint {

        static final CountDownLatch CLOSED_LATCH = new CountDownLatch(1);

        @OnOpen
        String open(WebSocketConnection connection) {
            return connection.id();
        }

        @OnClose
        void close() {
            CLOSED_LATCH.countDown();
        }

    }

}
