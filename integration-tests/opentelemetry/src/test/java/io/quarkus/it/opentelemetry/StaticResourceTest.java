package io.quarkus.it.opentelemetry;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
public class StaticResourceTest {
    @BeforeEach
    void setUp() {
        resetExporter();
    }

    @Test
    void staticResource() {
        given()
                .when()
                .get("/test.html")
                .then()
                .statusCode(200)
                .body(containsString("<html><body>Test</body></html>"));

        List<Map<String, Object>> spans = getSpans();
        assertTrue(spans.isEmpty());
    }

    private static void resetExporter() {
        given()
                .when().get("/export/clear")
                .then()
                .statusCode(204);
    }

    private static List<Map<String, Object>> getSpans() {
        return get("/export").body().as(new TypeRef<>() {
        });
    }
}
