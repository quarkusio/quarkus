package io.quarkus.websockets.next.test.traffic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocketClient;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnector;

public class ClientEndpointTrafficLoggerTest extends TrafficLoggerTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Endpoint.class, Client.class);
                TrafficLoggerTest.addApplicationProperties(root, false);
            })
            .setLogRecordPredicate(
                    TrafficLoggerTest::isTrafficLogRecord)
            .assertLogRecords(logRecordsConsumer(true));

    @Inject
    WebSocketConnector<Client> connector;

    @Test
    public void testTrafficLogger() throws InterruptedException {
        WebSocketClientConnection conn = connector
                .baseUri(endUri)
                .connectAndAwait();
        assertTrue(Client.MESSAGE_LATCH.await(5, TimeUnit.SECONDS));
        assertEquals("ok", Client.MESSAGES.get(0));
        conn.closeAndAwait();
        assertTrue(Client.CLOSED_LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(Endpoint.CLOSED_LATCH.await(5, TimeUnit.SECONDS));
    }

    @WebSocketClient(path = "/end")
    public static class Client {

        static final List<String> MESSAGES = new CopyOnWriteArrayList<>();

        static final CountDownLatch MESSAGE_LATCH = new CountDownLatch(1);

        static final CountDownLatch CLOSED_LATCH = new CountDownLatch(1);

        @OnTextMessage
        void onMessage(String message) {
            MESSAGES.add(message);
            MESSAGE_LATCH.countDown();
        }

        @OnClose
        void onClose() {
            CLOSED_LATCH.countDown();
        }

    }

}
