package io.quarkus.websockets.next.test.telemetry;

import jakarta.inject.Inject;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.websockets.next.WebSocketConnector;
import io.quarkus.websockets.next.test.telemetry.endpoints.onbinarymessage.BounceClient;
import io.quarkus.websockets.next.test.telemetry.endpoints.onbinarymessage.MultiClient;

/**
 * Tests metrics for {@link io.quarkus.websockets.next.OnBinaryMessage}.
 */
public class MicrometerWebSocketsOnBinaryMessageTest extends AbstractWebSocketsOnMessageTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = createQuarkusUnitTest(
            "io.quarkus.websockets.next.test.telemetry.endpoints.onbinarymessage");

    @Inject
    WebSocketConnector<BounceClient> bounceClientConnector;

    @Inject
    WebSocketConnector<MultiClient> multiClientConnector;

    @Override
    protected boolean binaryMode() {
        return true;
    }

    @Override
    protected WebSocketConnector<?> bounceClientConnector() {
        return bounceClientConnector;
    }

    @Override
    protected WebSocketConnector<?> multiClientConnector() {
        return multiClientConnector;
    }
}
