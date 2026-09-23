package io.quarkus.websockets.next.test.errors;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.TextDecodeException;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.smallrye.mutiny.Multi;
import io.vertx.core.Vertx;

public class MultiTextDecodeErrorBackpressureTest {

    @RegisterExtension
    public static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> {
                root.addClasses(Echo.class, WSClient.class);
            })
            // Bound the number of in-flight messages to a single message so that a leaked fetch(1) stalls the stream
            .overrideConfigKey("quarkus.websockets-next.server.max-pending-messages", "1");

    @Inject
    Vertx vertx;

    @TestHTTPResource("echo")
    URI testUri;

    @Test
    void testDecodeErrorDoesNotStallStream() {
        WSClient client = WSClient.create(vertx).connect(testUri);
        // Send more messages that fail to decode than the configured max-pending-messages limit.
        // A decoded item never reaches the Multi, so the emission-tied fetch(1) never fires; without the
        // fix that requests one more message after a failed decode, the stream would stall after the first
        // failure and the subsequent messages would never be delivered.
        client.send("not a json 1");
        client.send("not a json 2");
        client.send("not a json 3");
        client.waitForMessages(3);
        // The @OnError responses may be delivered in any order
        assertThat(client.getMessages().stream().map(Object::toString)).containsExactlyInAnyOrder(
                "Problem decoding: not a json 1",
                "Problem decoding: not a json 2",
                "Problem decoding: not a json 3");
    }

    @WebSocket(path = "/echo")
    public static class Echo {

        @OnTextMessage
        Multi<Pojo> process(Multi<Pojo> pojos) {
            return pojos;
        }

        @OnError
        String decodingError(TextDecodeException e) {
            return "Problem decoding: " + e.getText();
        }

    }

    public static class Pojo {

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

}
