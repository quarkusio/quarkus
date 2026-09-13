package io.quarkus.websockets.test.handshake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

/**
 * Without {@code quarkus.websocket.dispatch-to-worker} the handshake configurator keeps running on the event loop.
 */
public class HandshakeOnEventLoopByDefaultTest {

    @TestHTTPResource("handshake")
    URI uri;

    @RegisterExtension
    public static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(a -> a
                    .addClasses(HandshakeWebSocket.class, HandshakeRecordingConfigurator.class, HandshakeClient.class));

    @Test
    public void handshakeRunsOnTheEventLoop() throws Exception {
        HandshakeClient.Result result = HandshakeClient.connectAndEcho(uri);
        assertTrue(HandshakeRecordingConfigurator.ON_EVENT_LOOP.get(), HandshakeRecordingConfigurator.THREAD.get());
        assertEquals(HandshakeRecordingConfigurator.THREAD.get(),
                result.responseHeaders().get(HandshakeRecordingConfigurator.HEADER).get(0));
    }
}
