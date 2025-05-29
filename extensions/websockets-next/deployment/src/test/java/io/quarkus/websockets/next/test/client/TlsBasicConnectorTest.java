package io.quarkus.websockets.next.test.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "ssl-test", password = "secret", formats = {
        Format.JKS, Format.PKCS12, Format.PEM }))
public class TlsBasicConnectorTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(ServerEndpoint.class)
                    .addAsResource(new File("target/certs/ssl-test-keystore.jks"), "keystore.jks")
                    .addAsResource(new File("target/certs/ssl-test-truststore.jks"), "truststore.jks"))
            .overrideConfigKey("quarkus.tls.key-store.jks.path", "keystore.jks")
            .overrideConfigKey("quarkus.tls.key-store.jks.password", "secret")
            .overrideConfigKey("quarkus.tls.ws-client.trust-store.jks.path", "truststore.jks")
            .overrideConfigKey("quarkus.tls.ws-client.trust-store.jks.password", "secret")
            .overrideConfigKey("quarkus.websockets-next.client.tls-configuration-name", "ws-client");

    @Inject
    BasicWebSocketConnector connector;

    @TestHTTPResource(value = "/end", tls = true)
    URI uri;

    @Test
    void testClient() throws URISyntaxException {
        assertClient(uri);
        URI wssUri = new URI("wss", uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(),
                uri.getFragment());
        assertClient(wssUri);
    }

    void assertClient(URI uri) {
        WebSocketClientConnection connection = connector
                .baseUri(uri)
                .path("/{name}")
                .pathParam("name", "Lu")
                .connectAndAwait();
        assertTrue(connection.isOpen());
        assertTrue(connection.isSecure());
        connection.closeAndAwait();
    }

    @WebSocket(path = "/end/{name}")
    public static class ServerEndpoint {

        @OnOpen
        void open() {
        }

    }

}
