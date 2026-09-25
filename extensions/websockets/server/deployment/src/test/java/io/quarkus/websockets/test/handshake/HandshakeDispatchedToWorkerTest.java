package io.quarkus.websockets.test.handshake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

/**
 * With {@code quarkus.websocket.dispatch-to-worker=true} the handshake configurator runs on a worker thread, so it
 * may block, and the headers it adds still reach the client.
 */
public class HandshakeDispatchedToWorkerTest {

    @TestHTTPResource("handshake")
    URI uri;

    @RegisterExtension
    public static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(a -> a
                    .addClasses(HandshakeWebSocket.class, HandshakeRecordingConfigurator.class, HandshakeClient.class)
                    .add(new StringAsset("quarkus.websocket.dispatch-to-worker=true"), "application.properties"));

    @Test
    public void handshakeRunsOnAWorkerThread() throws Exception {
        HandshakeClient.Result result = HandshakeClient.connectAndEcho(uri);
        assertNotNull(HandshakeRecordingConfigurator.THREAD.get());
        assertFalse(HandshakeRecordingConfigurator.ON_EVENT_LOOP.get(), HandshakeRecordingConfigurator.THREAD.get());
        assertEquals(HandshakeRecordingConfigurator.THREAD.get(),
                result.responseHeaders().get(HandshakeRecordingConfigurator.HEADER).get(0));
    }
}
