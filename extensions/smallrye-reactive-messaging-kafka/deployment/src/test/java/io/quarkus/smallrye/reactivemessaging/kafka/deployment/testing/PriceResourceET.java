package io.quarkus.smallrye.reactivemessaging.kafka.deployment.testing;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.sse.SseEventSource;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class PriceResourceET {

    @Test
    public void sseStream() {
        Awaitility.await().untilAsserted(() -> {
            RestAssured.get("http://localhost:8081/prices").then().statusCode(200);
        });

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://localhost:8081/prices/stream");

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
