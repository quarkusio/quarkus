package io.quarkus.it.opentelemetry;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class OpenTelemetryTest extends BaseTest {

    @BeforeEach
    void reset() {
        await().atMost(5, SECONDS).until(() -> {
            List<Map<String, Object>> spans = getSpans();
            if (spans.size() == 0) {
                return true;
            } else {
                given().get("/reset").then().statusCode(HTTP_OK);
                return false;
            }
        });
    }

    @Test
    void buildTimeEnabled() {
        given()
                .when().get("/hello")
                .then()
                .statusCode(200)
                .body(is("Hello from Quarkus REST"));
        await().atMost(5, SECONDS).until(() -> getSpans().size() == 1);
    }
}
