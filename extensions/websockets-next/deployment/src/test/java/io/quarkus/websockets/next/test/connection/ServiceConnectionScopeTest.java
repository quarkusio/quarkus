package io.quarkus.websockets.next.test.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;

public class ServiceConnectionScopeTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(MyEndpoint.class, WSClient.class);
            });

    @Inject
    MyEndpoint endpoint;

    @Inject
    Vertx vertx;

    @TestHTTPResource("/")
    URI baseUri;

    @Test
    void verifyThatConnectionIsNotAccessibleOutsideOfTheSessionScope() {
        endpoint.testConnectionNotAccessibleOutsideOfWsMethods();
    }

    @Test
    void verifyThatConnectionIsAccessibleInSessionScope() {
        WSClient client = WSClient.create(vertx);
        var resp = client.connect(WSClient.toWS(baseUri, "/ws"))
                .sendAndAwaitReply("hello");
        assertThat(resp.toString()).isEqualTo("HELLO");
    }

    @WebSocket(path = "/ws")
    public static class MyEndpoint {

        @Inject
        WebSocketConnection connection;

        @OnTextMessage
        public String onMessage(String message) {
            assertNotNull(Arc.container().getActiveContext(SessionScoped.class));
            // By default, the request context is only activated if needed
            assertNull(Arc.container().getActiveContext(RequestScoped.class));
            assertNotNull(connection.id());
            return message.toUpperCase();
        }

        @ActivateRequestContext
        void testConnectionNotAccessibleOutsideOfWsMethods() {
            assertNull(Arc.container().getActiveContext(SessionScoped.class));
            assertNotNull(Arc.container().getActiveContext(RequestScoped.class));
            // WebSocketConnection is @SessionScoped
            assertThrows(ContextNotActiveException.class, () -> connection.id());
        }

    }

}
