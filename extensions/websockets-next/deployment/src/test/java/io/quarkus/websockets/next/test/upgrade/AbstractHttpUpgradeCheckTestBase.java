package io.quarkus.websockets.next.test.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;
import io.vertx.core.http.UpgradeRejectedException;
import io.vertx.core.http.WebSocketConnectOptions;

public abstract class AbstractHttpUpgradeCheckTestBase {

    @Inject
    Vertx vertx;

    @TestHTTPResource("opening")
    URI openingUri;

    @TestHTTPResource("responding")
    URI respondingUri;

    @TestHTTPResource("rejecting")
    URI rejectingUri;

    @BeforeEach
    public void cleanUp() {
        Opening.OPENED.set(false);
        OpeningHttpUpgradeCheck.INVOKED.set(0);
    }

    @Test
    public void testHttpUpgradeRejected() {
        try (WSClient client = new WSClient(vertx)) {
            CompletionException ce = assertThrows(CompletionException.class,
                    () -> client.connect(
                            new WebSocketConnectOptions().addHeader(RejectingHttpUpgradeCheck.REJECT_HEADER, "ignored"),
                            rejectingUri));
            Throwable root = ExceptionUtil.getRootCause(ce);
            assertInstanceOf(UpgradeRejectedException.class, root);
            assertTrue(root.getMessage().contains("403"), root.getMessage());
        }
    }

    @Test
    public void testHttpUpgradePermitted() {
        try (WSClient client = new WSClient(vertx)) {
            client.connect(openingUri);
            Awaitility.await().atMost(Duration.ofSeconds(2)).until(() -> OpeningHttpUpgradeCheck.INVOKED.get() == 1);
        }
    }

    @Test
    public void testHttpUpgradeOkAndResponding() {
        // test no HTTP Upgrade check rejected the upgrade or recorded value
        try (WSClient client = new WSClient(vertx)) {
            client.connect(new WebSocketConnectOptions(), respondingUri);
            var response = client.sendAndAwaitReply("Ho").toString();
            assertEquals("Ho Hey", response);
            assertEquals(0, OpeningHttpUpgradeCheck.INVOKED.get());
        }
    }

    @WebSocket(path = "/rejecting", endpointId = "rejecting-id")
    public static class Rejecting {

        @OnTextMessage
        public void onMessage(String message) {
            // do nothing
        }

    }

    @WebSocket(path = "/opening", endpointId = "opening-id")
    public static class Opening {

        static final AtomicBoolean OPENED = new AtomicBoolean(false);

        @OnTextMessage
        public void onMessage(String message) {
            // do nothing
        }

        @OnOpen
        void onOpen() {
            OPENED.set(true);
        }

    }

    @WebSocket(path = "/responding", endpointId = "closing-id")
    public static class Responding {

        @OnTextMessage
        public String onMessage(String message) {
            return message + " Hey";
        }

    }
}
