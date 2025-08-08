package io.quarkus.websockets.next.test.errors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;

public class MixedErrorHandlersTest {
    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Echo.class, GlobalErrorHandlers.class, WSClient.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("echo")
    URI testUri;

    @Test
    void test() {
        WSClient client = WSClient.create(vertx).connect(testUri);

        client.send("0");
        assertEquals("OK", client.waitForNextMessage().toString());

        client.send("1");
        assertEquals("IAE", client.waitForNextMessage().toString());

        client.send("2");
        assertEquals("ISE", client.waitForNextMessage().toString());

        client.send("3");
        assertEquals("RE", client.waitForNextMessage().toString());

        client.send("4");
        assertEquals("E", client.waitForNextMessage().toString());

        client.send("5");
        assertEquals("OK", client.waitForNextMessage().toString());
    }

    @WebSocket(path = "/echo")
    public static class Echo {
        @OnTextMessage
        String echo(String msg) throws Exception {
            if ("1".equals(msg)) {
                throw new IllegalArgumentException();
            } else if ("2".equals(msg)) {
                throw new IllegalStateException();
            } else if ("3".equals(msg)) {
                throw new RuntimeException();
            } else if ("4".equals(msg)) {
                throw new Exception();
            } else {
                return "OK";
            }
        }

        @OnError
        String handle(IllegalArgumentException e) {
            return "IAE";
        }

        @OnError
        String handle(RuntimeException e) {
            return "RE";
        }
    }

    @ApplicationScoped
    public static class GlobalErrorHandlers {
        @OnError
        String handle(IllegalStateException e) {
            return "ISE";
        }

        @OnError
        String handle(Exception e) {
            return "E";
        }
    }
}
