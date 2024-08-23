package io.quarkus.it.opentelemetry;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
public class StaticResourceTest {

    @BeforeEach
    @AfterEach
    void reset() {
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
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

    private static List<Map<String, Object>> getSpans() {
        return get("/export").body().as(new TypeRef<>() {
        });
    }
}
