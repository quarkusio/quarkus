package io.quarkus.resteasy.reactive.server.test.sse;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.sse.SseEventSource;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class SseBroadcasterCloseTestCase {

    @TestHTTPResource
    URI uri;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SseBroadcasterCloseResource.class));

    @Test
    public void onCloseFiresExactlyOncePerSinkWhenBroadcasterClosed() throws Exception {
        Client client = ClientBuilder.newBuilder().build();
        try {
            // Open an SSE connection so a sink is registered with the broadcaster.
            WebTarget registerTarget = client.target(uri.toString() + "sse-broadcaster/register");
            CountDownLatch connected = new CountDownLatch(1);
            try (SseEventSource source = SseEventSource.target(registerTarget).build()) {
                source.register(event -> connected.countDown());
                source.open();
                Assertions.assertTrue(connected.await(10, TimeUnit.SECONDS),
                        "client did not receive the initial SSE event");

                // Close the broadcaster on the server side.
                client.target(uri.toString() + "sse-broadcaster/close").request().get(String.class);

                // The registered sink's onClose listener must fire exactly once, not twice.
                Awaitility.await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> Assertions.assertEquals("1",
                        client.target(uri.toString() + "sse-broadcaster/close-count").request().get(String.class)));
            }
        } finally {
            client.close();
        }
    }
}
