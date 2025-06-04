package io.quarkus.websockets.next.test.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
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

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "ssl-test", password = "secret", formats = {
        Format.JKS, Format.PKCS12, Format.PEM }))
public class TlsClientEndpointTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ServerEndpoint.class, ClientEndpoint.class)
                    .addAsResource(new File("target/certs/ssl-test-keystore.jks"), "keystore.jks")
                    .addAsResource(new File("target/certs/ssl-test-truststore.jks"), "truststore.jks"))
            .overrideConfigKey("quarkus.tls.key-store.jks.path", "keystore.jks")
            .overrideConfigKey("quarkus.tls.key-store.jks.password", "secret")
            .overrideConfigKey("quarkus.tls.ws-client.trust-store.jks.path", "truststore.jks")
            .overrideConfigKey("quarkus.tls.ws-client.trust-store.jks.password", "secret")
            .overrideConfigKey("quarkus.websockets-next.client.tls-configuration-name", "ws-client");

    @Inject
    WebSocketConnector<ClientEndpoint> connector;

    @TestHTTPResource(value = "/", tls = true)
    URI uri;

    @Test
    void testClient() throws InterruptedException, SSLPeerUnverifiedException, URISyntaxException {
        assertClient(uri);
        URI wssUri = new URI("wss", uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(),
                uri.getFragment());
        assertClient(wssUri);
    }

    void assertClient(URI uri) throws InterruptedException, SSLPeerUnverifiedException {
        WebSocketClientConnection connection = connector
                .baseUri(uri)
                // The value will be encoded automatically
                .pathParam("name", "Lu=")
                .connectAndAwait();
        assertTrue(connection.isSecure());
        assertNotNull(connection.sslSession());
        assertNull(connection.sslSession().getLocalPrincipal());
        assertNull(connection.sslSession().getLocalCertificates());
        assertNotNull(connection.sslSession().getPeerPrincipal());
        assertNotNull(connection.sslSession().getPeerCertificates());

        assertTrue(ServerEndpoint.openedLatch.await(5, TimeUnit.SECONDS));
        assertTrue(ServerEndpoint.CONNECTION_REF.get().isSecure());
        assertNotNull(ServerEndpoint.CONNECTION_REF.get().sslSession());
        assertNotNull(ServerEndpoint.CONNECTION_REF.get().sslSession().getLocalPrincipal());
        assertNotNull(ServerEndpoint.CONNECTION_REF.get().sslSession().getLocalCertificates());
        assertThrows(SSLPeerUnverifiedException.class,
                () -> ServerEndpoint.CONNECTION_REF.get().sslSession().getPeerPrincipal());
        assertThrows(SSLPeerUnverifiedException.class,
                () -> ServerEndpoint.CONNECTION_REF.get().sslSession().getPeerCertificates());
        assertEquals(connection.sslSession().getPeerPrincipal(),
                ServerEndpoint.CONNECTION_REF.get().sslSession().getLocalPrincipal());
        assertArrayEquals(connection.sslSession().getPeerCertificates(),
                ServerEndpoint.CONNECTION_REF.get().sslSession().getLocalCertificates());

        assertEquals("Lu=", connection.pathParam("name"));
        connection.sendTextAndAwait("Hi!");

        assertTrue(ClientEndpoint.messageLatch.await(5, TimeUnit.SECONDS));
        assertEquals("Lu=:Hello Lu=!", ClientEndpoint.MESSAGES.get(0));
        assertEquals("Lu=:Hi!", ClientEndpoint.MESSAGES.get(1));

        connection.closeAndAwait();
        assertTrue(ClientEndpoint.closedLatch.await(5, TimeUnit.SECONDS));
        assertTrue(ServerEndpoint.closedLatch.await(5, TimeUnit.SECONDS));

        ServerEndpoint.reset();
        ClientEndpoint.reset();
    }

    @WebSocket(path = "/endpoint/{name}")
    public static class ServerEndpoint {

        static final AtomicReference<WebSocketConnection> CONNECTION_REF = new AtomicReference<>();

        static volatile CountDownLatch openedLatch = new CountDownLatch(1);

        static volatile CountDownLatch closedLatch = new CountDownLatch(1);

        @OnOpen
        String open(@PathParam String name, WebSocketConnection connection) {
            CONNECTION_REF.set(connection);
            openedLatch.countDown();
            return "Hello " + name + "!";
        }

        @OnTextMessage
        String echo(String message) {
            return message;
        }

        @OnClose
        void close() {
            closedLatch.countDown();
        }

        static void reset() {
            CONNECTION_REF.set(null);
            openedLatch = new CountDownLatch(1);
            closedLatch = new CountDownLatch(1);
        }

    }

    @WebSocketClient(path = "/endpoint/{name}")
    public static class ClientEndpoint {

        static volatile CountDownLatch messageLatch = new CountDownLatch(2);

        static final List<String> MESSAGES = new CopyOnWriteArrayList<>();

        static volatile CountDownLatch closedLatch = new CountDownLatch(1);

        @OnTextMessage
        void onMessage(@PathParam String name, String message, WebSocketClientConnection connection) {
            if (!name.equals(connection.pathParam("name"))) {
                throw new IllegalArgumentException();
            }
            MESSAGES.add(name + ":" + message);
            messageLatch.countDown();
        }

        @OnClose
        void close() {
            closedLatch.countDown();
        }

        static void reset() {
            MESSAGES.clear();
            messageLatch = new CountDownLatch(2);
            closedLatch = new CountDownLatch(1);
        }

    }
}
