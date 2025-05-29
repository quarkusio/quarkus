package io.quarkus.websockets.next.test.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLPeerUnverifiedException;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.PathParam;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketClient;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.WebSocketConnector;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "mtls-test", password = "secret", formats = {
        Format.JKS, Format.PKCS12, Format.PEM }, client = true))
public class MtlsWithP12ClientEndpointTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ServerEndpoint.class, ClientEndpoint.class)
                    .addAsResource(new File("target/certs/mtls-test-keystore.p12"), "server-keystore.p12")
                    .addAsResource(new File("target/certs/mtls-test-server-truststore.p12"), "server-truststore.p12")
                    .addAsResource(new File("target/certs/mtls-test-client-keystore.p12"), "client-keystore.p12")
                    .addAsResource(new File("target/certs/mtls-test-client-truststore.p12"), "client-truststore.p12"))

            .overrideConfigKey("quarkus.tls.ws-server.key-store.p12.path", "server-keystore.p12")
            .overrideConfigKey("quarkus.tls.ws-server.key-store.p12.password", "secret")
            .overrideConfigKey("quarkus.tls.ws-server.trust-store.p12.path", "server-truststore.p12")
            .overrideConfigKey("quarkus.tls.ws-server.trust-store.p12.password", "secret")
            .overrideConfigKey("quarkus.http.tls-configuration-name", "ws-server")
            .overrideConfigKey("quarkus.http.ssl.client-auth", "required")

            .overrideConfigKey("quarkus.tls.ws-client.key-store.p12.path", "client-keystore.p12")
            .overrideConfigKey("quarkus.tls.ws-client.key-store.p12.password", "secret")
            .overrideConfigKey("quarkus.tls.ws-client.trust-store.p12.path", "client-truststore.p12")
            .overrideConfigKey("quarkus.tls.ws-client.trust-store.p12.password", "secret")
            .overrideConfigKey("quarkus.websockets-next.client.tls-configuration-name", "ws-client");

    @Inject
    WebSocketConnector<ClientEndpoint> connector;

    @TestHTTPResource(value = "/", tls = true)
    URI uri;

    @Test
    void testClient() throws InterruptedException, SSLPeerUnverifiedException {
        WebSocketClientConnection connection = connector
                .baseUri(uri)
                // The value will be encoded automatically
                .pathParam("name", "Lu=")
                .connectAndAwait();
        assertTrue(connection.isSecure());
        assertNotNull(connection.sslSession());
        assertNotNull(connection.sslSession().getLocalPrincipal());
        assertNotNull(connection.sslSession().getLocalCertificates());
        assertNotNull(connection.sslSession().getPeerPrincipal());
        assertNotNull(connection.sslSession().getPeerCertificates());

        assertTrue(ServerEndpoint.OPENED_LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(ServerEndpoint.CONNECTION_REF.get().isSecure());
        assertNotNull(ServerEndpoint.CONNECTION_REF.get().sslSession());
        assertNotNull(ServerEndpoint.CONNECTION_REF.get().sslSession().getLocalPrincipal());
        assertNotNull(ServerEndpoint.CONNECTION_REF.get().sslSession().getLocalCertificates());
        assertNotNull(ServerEndpoint.CONNECTION_REF.get().sslSession().getPeerPrincipal());
        assertNotNull(ServerEndpoint.CONNECTION_REF.get().sslSession().getPeerCertificates());
        assertEquals(connection.sslSession().getPeerPrincipal(),
                ServerEndpoint.CONNECTION_REF.get().sslSession().getLocalPrincipal());
        assertArrayEquals(connection.sslSession().getPeerCertificates(),
                ServerEndpoint.CONNECTION_REF.get().sslSession().getLocalCertificates());
        assertEquals(connection.sslSession().getLocalPrincipal(),
                ServerEndpoint.CONNECTION_REF.get().sslSession().getPeerPrincipal());
        assertArrayEquals(connection.sslSession().getLocalCertificates(),
                ServerEndpoint.CONNECTION_REF.get().sslSession().getPeerCertificates());

        assertEquals("Lu=", connection.pathParam("name"));
        connection.sendTextAndAwait("Hi!");

        assertTrue(ClientEndpoint.MESSAGE_LATCH.await(5, TimeUnit.SECONDS));
        assertEquals("Lu=:Hello Lu=!", ClientEndpoint.MESSAGES.get(0));
        assertEquals("Lu=:Hi!", ClientEndpoint.MESSAGES.get(1));

        connection.closeAndAwait();
        assertTrue(ClientEndpoint.CLOSED_LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(ServerEndpoint.CLOSED_LATCH.await(5, TimeUnit.SECONDS));
    }

    @WebSocket(path = "/endpoint/{name}")
    public static class ServerEndpoint {

        static final AtomicReference<WebSocketConnection> CONNECTION_REF = new AtomicReference<>();

        static final CountDownLatch OPENED_LATCH = new CountDownLatch(1);

        static final CountDownLatch CLOSED_LATCH = new CountDownLatch(1);

        @OnOpen
        String open(@PathParam String name, WebSocketConnection connection) {
            CONNECTION_REF.set(connection);
            OPENED_LATCH.countDown();
            return "Hello " + name + "!";
        }

        @OnTextMessage
        String echo(String message) {
            return message;
        }

        @OnClose
        void close() {
            CLOSED_LATCH.countDown();
        }

    }

    @WebSocketClient(path = "/endpoint/{name}")
    public static class ClientEndpoint {

        static final CountDownLatch MESSAGE_LATCH = new CountDownLatch(2);

        static final List<String> MESSAGES = new CopyOnWriteArrayList<>();

        static final CountDownLatch CLOSED_LATCH = new CountDownLatch(1);

        @OnTextMessage
        void onMessage(@PathParam String name, String message, WebSocketClientConnection connection) {
            if (!name.equals(connection.pathParam("name"))) {
                throw new IllegalArgumentException();
            }
            MESSAGES.add(name + ":" + message);
            MESSAGE_LATCH.countDown();
        }

        @OnClose
        void close() {
            CLOSED_LATCH.countDown();
        }

    }
}
