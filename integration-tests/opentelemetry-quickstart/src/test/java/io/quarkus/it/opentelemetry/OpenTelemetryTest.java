package io.quarkus.it.opentelemetry;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class OpenTelemetryTest extends BaseTest {
    @Test
    void buildTimeEnabled() {
        given()
                .when().get("/hello")
                .then()
                .statusCode(200)
                .body(is("Hello from RESTEasy Reactive"));
        await().atMost(5, SECONDS).until(() -> getSpans().size() == 1);
    }
}
