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
import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.vertx.core.Context;

public class BasicConnectorTrafficLoggerTest extends TrafficLoggerTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Endpoint.class);
                TrafficLoggerTest.addApplicationProperties(root, false);
            })
            .setLogRecordPredicate(TrafficLoggerTest::isTrafficLogRecord)
            .assertLogRecords(logRecordsConsumer(true));

    @Inject
    BasicWebSocketConnector connector;

    @Test
    public void testTrafficLogger() throws InterruptedException {
        List<String> messages = new CopyOnWriteArrayList<>();
        CountDownLatch closedLatch = new CountDownLatch(1);
        CountDownLatch messageLatch = new CountDownLatch(1);
        WebSocketClientConnection conn = connector
                .baseUri(endUri)
                .path("end")
                .onTextMessage((c, m) -> {
                    assertTrue(Context.isOnWorkerThread());
                    messages.add(m);
                    messageLatch.countDown();
                })
                .onClose((c, s) -> closedLatch.countDown())
                .connectAndAwait();
        conn.sendTextAndAwait("dummy");
        assertTrue(messageLatch.await(5, TimeUnit.SECONDS));
        assertEquals("ok", messages.get(0));
        conn.closeAndAwait();
        assertTrue(closedLatch.await(5, TimeUnit.SECONDS));
    }

}
