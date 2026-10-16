package io.quarkus.websockets.next.test.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.quarkus.websockets.next.HandshakeRequest;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketClientConnection;

class BasicConnectorBaseUriPathTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(
                    root -> root.addClasses(NoTrailingSlashEndpoint.class, TrailingSlashEndpoint.class, RootEndpoint.class));

    @TestHTTPResource("/no-trailing-slash?id=1")
    URI noTrailingSlashUri;

    @TestHTTPResource("/trailing-slash/?id=1")
    URI trailingSlashUri;

    @TestHTTPResource("/")
    URI rootUri;

    @Test
    void doesNotAppendTrailingSlashToBaseUriPath() {
        assertRequestUri(noTrailingSlashUri, "/no-trailing-slash?id=1");
    }

    @Test
    void keepsTrailingSlashOfBaseUriPath() {
        assertRequestUri(trailingSlashUri, "/trailing-slash/?id=1");
    }

    @Test
    void sendsRootPathWhenBaseUriHasNoPath() {
        assertRequestUri(URI.create("http://" + rootUri.getAuthority() + "?id=1"), "/?id=1");
    }

    private static void assertRequestUri(URI baseUri, String expectedRequestUri) {
        CompletableFuture<String> serverRequestUri = new CompletableFuture<>();
        WebSocketClientConnection connection = BasicWebSocketConnector.create()
                .baseUri(baseUri)
                .onTextMessage((c, m) -> serverRequestUri.complete(m))
                .connectAndAwait();
        assertThat(serverRequestUri).succeedsWithin(Duration.ofSeconds(5)).isEqualTo(expectedRequestUri);
        assertThat(requestUri(connection.handshakeRequest())).isEqualTo(expectedRequestUri);
        connection.closeAndAwait();
    }

    private static String requestUri(HandshakeRequest handshakeRequest) {
        return handshakeRequest.path() + "?" + handshakeRequest.query();
    }

    @WebSocket(path = "/no-trailing-slash")
    static class NoTrailingSlashEndpoint {

        @OnOpen
        String open(HandshakeRequest handshakeRequest) {
            return requestUri(handshakeRequest);
        }

    }

    @WebSocket(path = "/trailing-slash/")
    static class TrailingSlashEndpoint {

        @OnOpen
        String open(HandshakeRequest handshakeRequest) {
            return requestUri(handshakeRequest);
        }

    }

    @WebSocket(path = "/")
    static class RootEndpoint {

        @OnOpen
        String open(HandshakeRequest handshakeRequest) {
            return requestUri(handshakeRequest);
        }

    }

}
