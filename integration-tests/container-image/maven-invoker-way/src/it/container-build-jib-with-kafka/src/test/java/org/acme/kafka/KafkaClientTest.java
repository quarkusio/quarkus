package org.acme.kafka;

import io.quarkus.test.junit.DisabledOnNativeImage;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.StringContains.containsString;

@QuarkusTest
class KafkaClientTest {

    @Test
    void testBareClients() {
        given()
                .queryParam("key", "my-key")
                .queryParam("value", "my-value")
                .when()
                .post("/kafka")
                .then()
                .statusCode(200);

        await()
                .atMost(Duration.ofMinutes(1))
                .untilAsserted(() ->
                        get("/kafka")
                                .then()
                                .statusCode(200)
                                .body(containsString("my-key-my-value"))
                );

        get("/kafka/topics")
                .then()
                .statusCode(200)
                .body(containsString("hello"));
    }
}
