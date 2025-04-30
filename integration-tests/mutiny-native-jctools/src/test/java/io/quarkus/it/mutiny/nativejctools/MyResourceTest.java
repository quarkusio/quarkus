package io.quarkus.it.mutiny.nativejctools;

import static io.restassured.RestAssured.get;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.sse.SseEventSource;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class MyResourceTest {

    @Test
    public void testCreateQueues() {
        get("/tests/create-queues")
                .then()
                .body(is("Ok :: 523776/523776/523776/523776/523776/523776"))
                .statusCode(200);
    }

    @Test
    public void testTicksWithOverflow() {
        get("/tests/ticks-overflow")
                .then()
                .body(is("::::::::::"))
                .statusCode(200);
    }

    @Test
    public void testEmitter() throws Throwable {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://localhost:" + RestAssured.port + "/tests/emitter");
        SseEventSource eventSource = SseEventSource.target(target).build();

        AtomicBoolean done = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        ArrayList<String> events = new ArrayList<>();

        eventSource.register(
                event -> events.add(event.readData()),
                failure::set,
                () -> done.set(true));
        eventSource.open();

        await().atMost(Duration.ofSeconds(10))
                .until(() -> done.get() || failure.get() != null);

        if (failure.get() != null) {
            throw failure.get();
        }

        ArrayList<String> expected = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < i * 10; j++) {
                expected.add(String.valueOf(j));
            }
        }
        assertIterableEquals(expected, events);
    }
}
