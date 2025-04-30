package io.quarkus.websockets.next.test.errors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.smallrye.mutiny.Multi;
import io.vertx.core.Vertx;

public class MultiFailureTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Echo.class, WSClient.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("echo")
    URI testUri;

    @Test
    void testError() {
        WSClient client = WSClient.create(vertx).connect(testUri);
        client.sendAndAwait("bar,foo,baz");
        client.waitForMessages(2);
        assertEquals("bar", client.getMessages().get(0).toString());
        assertEquals("foo detected", client.getMessages().get(1).toString());
    }

    @WebSocket(path = "/echo")
    public static class Echo {

        @OnTextMessage
        Multi<String> process(String message) {
            return Multi.createFrom().items(message.split(",")).invoke(s -> {
                if (s.equals("foo")) {
                    throw new IllegalArgumentException();
                }
            });
        }

        @OnError
        String runtimeProblem(IllegalArgumentException e) {
            return "foo detected";
        }

    }

}
