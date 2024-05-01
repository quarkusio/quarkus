package io.quarkus.websockets.next.test.inboundprocessing;

import static io.quarkus.websockets.next.InboundProcessingMode.SERIAL;
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
import io.vertx.core.Vertx;

public class SerialInboundProcessingErrorTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Sim.class, WSClient.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("sim")
    URI simUri;

    @Test
    void testSerialExecution() {
        WSClient client = WSClient.create(vertx).connect(simUri);
        int messages = 100;
        for (int i = 0; i < messages; i++) {
            client.send(i + "");
        }
        client.waitForMessages(messages);
        for (int i = 0; i < messages; i++) {
            assertEquals(i + "", client.getMessages().get(i).toString());
        }
    }

    @WebSocket(path = "/sim", inboundProcessingMode = SERIAL)
    public static class Sim {

        @OnTextMessage
        String process(String message) throws InterruptedException {
            throw new IllegalArgumentException(message);
        }

        // error() should be always called before other messages from the queue are consumed by process()
        @OnError
        String error(IllegalArgumentException iae) {
            return iae.getMessage();
        }

    }

}
