package io.quarkus.smallrye.reactivemessaging.kafka.deployment.testing;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.SseEventSource;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class PriceResourceET {
    @TestHTTPEndpoint(PriceResource.class)
    @TestHTTPResource("/stream")
    URI uri;

    @Test
    public void sseStream() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(this.uri);

        List<Double> received = new CopyOnWriteArrayList<>();

        try (SseEventSource source = SseEventSource.target(target).build()) {
            source.register(inboundSseEvent -> received.add(Double.valueOf(inboundSseEvent.readData())));
            source.open();

            Awaitility.await()
                    .atMost(Duration.ofSeconds(1))
                    .until(() -> received.size() >= 2);
        }

        Assertions.assertThat(received)
                .hasSizeGreaterThanOrEqualTo(2)
                .allMatch(value -> (value >= 0) && (value < 100));
    }
}
