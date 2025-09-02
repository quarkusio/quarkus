package io.quarkus.websockets.next.test.client;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.net.URI;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketClient;
import io.quarkus.websockets.next.WebSocketConnector;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "ssl-test", password = "secret", formats = {
        Format.JKS, Format.PKCS12, Format.PEM }))
public class TlsClientEndpointMissingTlsConfigurationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ServerEndpoint.class, ClientEndpoint.class)
                    .addAsResource(new File("target/certs/ssl-test-keystore.jks"), "keystore.jks")
                    .addAsResource(new File("target/certs/ssl-test-truststore.jks"), "truststore.jks"))
            .overrideConfigKey("quarkus.tls.key-store.jks.path", "keystore.jks")
            .overrideConfigKey("quarkus.tls.key-store.jks.password", "secret")
            .overrideConfigKey("quarkus.websockets-next.client.tls-configuration-name", "ws-client");

    @Inject
    WebSocketConnector<ClientEndpoint> connector;

    @TestHTTPResource(value = "/", tls = true)
    URI uri;

    @Test
    void testClient() {
        assertThrows(IllegalStateException.class, () -> connector
                .baseUri(uri)
                .pathParam("name", "Lu=")
                .connectAndAwait());
    }

    @WebSocket(path = "/endpoint/{name}")
    public static class ServerEndpoint {

        @OnOpen
        void open() {
        }

    }

    @WebSocketClient(path = "/endpoint/{name}")
    public static class ClientEndpoint {

        @OnOpen
        void open() {
        }

    }
}
