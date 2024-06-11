package io.quarkus.websockets.next.test.traffic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;

public class ServerTrafficLoggerTest extends TrafficLoggerTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Endpoint.class, WSClient.class);
                TrafficLoggerTest.addApplicationProperties(root, true);
            })
            .setLogRecordPredicate(TrafficLoggerTest::isTrafficLogRecord)
            .assertLogRecords(logRecordsConsumer(false));

    @Inject
    Vertx vertx;

    @Test
    public void testTrafficLogger() throws InterruptedException, ExecutionException {
        try (WSClient client = new WSClient(vertx)) {
            client.connect(WSClient.toWS(endUri, "end"));
            client.waitForMessages(1);
            assertEquals("ok", client.getMessages().get(0).toString());
        }
        assertTrue(Endpoint.CLOSED_LATCH.await(5, TimeUnit.SECONDS));
    }

}
