package io.quarkus.websockets.next.test.args;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.PathParam;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;

public class PathParamArgumentTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(MontyEcho.class, WSClient.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("echo/monty")
    URI testUri;

    @Test
    void testArgument() {
        WSClient client = WSClient.create(vertx).connect(testUri);
        assertEquals("python:monty", client.sendAndAwaitReply("python").toString());
    }

    @WebSocket(path = "/echo/{grail}")
    public static class MontyEcho {

        @OnTextMessage
        String process(@PathParam String grail, String message) throws InterruptedException {
            return message + ":" + grail;
        }

    }

}
