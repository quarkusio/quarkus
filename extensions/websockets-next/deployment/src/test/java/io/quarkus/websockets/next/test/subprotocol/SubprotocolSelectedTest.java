package io.quarkus.websockets.next.test.subprotocol;

import static io.quarkus.websockets.next.HandshakeRequest.SEC_WEBSOCKET_PROTOCOL;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.concurrent.ExecutionException;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocketConnectOptions;

public class SubprotocolSelectedTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Endpoint.class, WSClient.class);
            }).overrideConfigKey("quarkus.websockets-next.server.supported-subprotocols", "oak,larch");

    @Inject
    Vertx vertx;

    @TestHTTPResource("endpoint")
    URI endUri;

    @Test
    void testSubprotocol() throws InterruptedException, ExecutionException {
        WSClient client = new WSClient(vertx).connect(new WebSocketConnectOptions().addSubProtocol("oak"), endUri);
        assertEquals("ok", client.waitForNextMessage().toString());
    }

    @WebSocket(path = "/endpoint")
    public static class Endpoint {

        @Inject
        WebSocketConnection connection;

        @OnOpen
        Uni<Void> open() {
            if (connection.handshakeRequest().header(SEC_WEBSOCKET_PROTOCOL) == null) {
                return connection.sendText("Sec-WebSocket-Protocol not set: " + connection.handshakeRequest().headers());
            } else if ("oak".equals(connection.subprotocol())) {
                return connection.sendText("ok");
            } else {
                return connection.sendText("Invalid protocol: " + connection.subprotocol());
            }
        }

    }

}
