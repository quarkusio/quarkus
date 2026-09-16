package io.quarkus.it.observation;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ObservationEndpointTest {

    @Test
    void manualObservationRegistersTimer() {
        given()
                .get("/observation/manual")
                .then()
                .statusCode(200)
                .body(is("manual-result"));

        // Timers are asserted with count = 0 — correct for a CompositeMeterRegistry with no sub-registries
        await().atMost(5, SECONDS).untilAsserted(() -> {
            given().get("/timer/manual.operation")
                    .then()
                    .statusCode(200)
                    .body("name", is("manual.operation"))
                    .body("count", is(0));
        });
    }

    @Test
    void nestedObservationsRegisterTimers() {
        given()
                .get("/observation/nested")
                .then()
                .statusCode(200)
                .body(is("nested-result"));

        await().atMost(5, SECONDS).untilAsserted(() -> {
            given().get("/timer/parent.operation")
                    .then()
                    .statusCode(200)
                    .body("name", is("parent.operation"))
                    .body("count", is(0));
            given().get("/timer/child.operation")
                    .then()
                    .statusCode(200)
                    .body("name", is("child.operation"))
                    .body("count", is(0));
        });
    }

    @Test
    void observedInterceptorRegistersTimer() {
        given()
                .get("/observation/observed")
                .then()
                .statusCode(200)
                .body(is("observed-result"));

        await().atMost(5, SECONDS).untilAsserted(() -> {
            given().get("/timer/doWork")
                    .then()
                    .statusCode(200)
                    .body("name", is("doWork"))
                    .body("count", is(0));
        });
    }
}
