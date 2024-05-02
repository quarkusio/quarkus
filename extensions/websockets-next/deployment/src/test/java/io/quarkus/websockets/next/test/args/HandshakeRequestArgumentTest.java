package io.quarkus.websockets.next.test.args;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.HandshakeRequest;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocketConnectOptions;

public class HandshakeRequestArgumentTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(XTest.class, WSClient.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("xtest")
    URI testUri;

    @Test
    void testArgument() {
        WSClient client = WSClient.create(vertx).connect(new WebSocketConnectOptions().addHeader("X-Test", "fool"),
                testUri);
        client.waitForMessages(1);
        assertEquals("fool", client.getLastMessage().toString());
    }

    @WebSocket(path = "/xtest")
    public static class XTest {

        @OnOpen
        String open(HandshakeRequest request) {
            return request.header("X-Test");
        }

    }

}
