package io.quarkus.resteasy.reactive.server.test.sse;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;
import jakarta.ws.rs.sse.SseEventSink;
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

    @Path("sse-broadcaster")
    public static class SseBroadcasterCloseResource {

        private static volatile SseBroadcaster broadcaster;
        private static final AtomicInteger onCloseCount = new AtomicInteger();

        @GET
        @Path("register")
        @Produces(MediaType.SERVER_SENT_EVENTS)
        public void register(SseEventSink eventSink, Sse sse) {
            broadcaster(sse).register(eventSink);
            eventSink.send(sse.newEvent("connected"));
        }

        @GET
        @Path("close")
        public String close() {
            SseBroadcaster current = broadcaster;
            if (current != null) {
                current.close();
            }
            return "OK";
        }

        @GET
        @Path("close-count")
        public String closeCount() {
            return String.valueOf(onCloseCount.get());
        }

        private static synchronized SseBroadcaster broadcaster(Sse sse) {
            if (broadcaster == null) {
                SseBroadcaster created = sse.newBroadcaster();
                created.onClose(sink -> onCloseCount.incrementAndGet());
                broadcaster = created;
            }
            return broadcaster;
        }
    }
}
