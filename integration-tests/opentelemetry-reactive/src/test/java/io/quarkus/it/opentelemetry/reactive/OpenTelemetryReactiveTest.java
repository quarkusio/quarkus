package io.quarkus.it.opentelemetry.reactive;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
public class OpenTelemetryReactiveTest {
    @AfterEach
    void reset() {
        given().get("/reset").then().statusCode(HTTP_OK);
        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 0);
    }

    @Test
    void get() {
        given()
                .contentType("application/json")
                .when()
                .queryParam("name", "Naruto")
                .get("/reactive")
                .then()
                .statusCode(200)
                .body(equalTo("Hello Naruto"));

        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 2);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(2, spans.size());
        assertEquals(spans.get(0).get("traceId"), spans.get(1).get("traceId"));
    }

    @Test
    void post() {
        given()
                .contentType("application/json")
                .when()
                .body("Naruto")
                .post("/reactive")
                .then()
                .statusCode(200)
                .body(equalTo("Hello Naruto"));

        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 2);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(2, spans.size());
        assertEquals(spans.get(0).get("traceId"), spans.get(1).get("traceId"));
    }

    private static List<Map<String, Object>> getSpans() {
        return when().get("/export").body().as(new TypeRef<>() {
        });
    }
}
