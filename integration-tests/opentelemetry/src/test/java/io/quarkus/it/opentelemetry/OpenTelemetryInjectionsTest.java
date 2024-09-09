package io.quarkus.it.opentelemetry;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
public class OpenTelemetryInjectionsTest {

    @BeforeEach
    @AfterEach
    void reset() {
        await().atMost(Duration.ofSeconds(30L)).until(() -> {
            // make sure spans are cleared
            List<Map<String, Object>> spans = getSpans();
            if (!spans.isEmpty()) {
                given().get("/reset").then().statusCode(HTTP_OK);
            }
            return spans.isEmpty();
        });
    }

    private List<Map<String, Object>> getSpans() {
        return get("/export").body().as(new TypeRef<>() {
        });
    }

    @Test
    public void testOTelInjections() {
        given()
                .when().get("/otel/injection")
                .then()
                .statusCode(200);
        await().atMost(5, SECONDS).until(() -> getSpans().size() == 1);
    }

    @Test
    public void testOTelInjectionsAsync() {
        given()
                .when().get("/otel/injection/async")
                .then()
                .statusCode(200);
        await().atMost(5, SECONDS).until(() -> getSpans().size() == 1);
    }
}
