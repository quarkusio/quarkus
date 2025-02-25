package io.quarkus.websockets.next.test.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;

public class ByteArrayBinaryMessageTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Endpont.class, WSClient.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("end")
    URI testUri;

    @Test
    public void testCodec() throws Exception {
        try (WSClient client = new WSClient(vertx)) {
            client.connect(testUri);
            client.send(Buffer.buffer("43"));
            client.waitForMessages(1);
            assertEquals("43", client.getMessages().get(0).toString());
        }
    }

    @WebSocket(path = "/end")
    public static class Endpont {

        // This is an equivalent to Sender#sendBinary(byte[])
        // byte[] is encoded with Buffer#buffer(byte[]), codec is not needed
        @OnBinaryMessage
        byte[] echo(Buffer message) {
            return message.getBytes();
        }

    }
}
