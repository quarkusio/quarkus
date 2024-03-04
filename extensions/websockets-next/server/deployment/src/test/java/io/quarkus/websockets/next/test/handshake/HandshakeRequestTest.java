package io.quarkus.websockets.next.test.handshake;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketServerConnection;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.json.JsonObject;

public class HandshakeRequestTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Head.class, WSClient.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("/")
    URI baseUri;

    @Test
    void testHandshake() {
        String header = "fool";
        String query = "name=Lu";
        WSClient client = WSClient.create(vertx).connect(new WebSocketConnectOptions().addHeader("X-Test", header),
                WSClient.toWS(baseUri, "/head?" + query));
        JsonObject reply = client.sendAndAwaitReply("1").toJsonObject();
        assertEquals(header, reply.getString("header"));
        assertEquals(header, reply.getJsonObject("headers").getString("X-Test".toLowerCase()),
                reply.getJsonObject("headers").toString());
        assertEquals(baseUri.getScheme(), reply.getString("scheme"));
        assertEquals(baseUri.getHost(), reply.getString("host"));
        assertEquals(baseUri.getPort(), reply.getInteger("port"));
        assertEquals("/head", reply.getString("path"));
        assertEquals(query, reply.getString("query"));
    }

    @WebSocket(path = "/head")
    public static class Head {

        @Inject
        WebSocketServerConnection connection;

        @OnMessage
        JsonObject process(String message) throws InterruptedException {
            JsonObject headers = new JsonObject();
            connection.handshakeRequest().headers().forEach((k, v) -> headers.put(k, v.get(0)));
            return new JsonObject()
                    .put("header", connection.handshakeRequest().header("X-Test"))
                    .put("headers", headers)
                    .put("scheme", connection.handshakeRequest().scheme())
                    .put("host", connection.handshakeRequest().host())
                    .put("port", connection.handshakeRequest().port())
                    .put("path", connection.handshakeRequest().path())
                    .put("query", connection.handshakeRequest().query());
        }

    }

}
