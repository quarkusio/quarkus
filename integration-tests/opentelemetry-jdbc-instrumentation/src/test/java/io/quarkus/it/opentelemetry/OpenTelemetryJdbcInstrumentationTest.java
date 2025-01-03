package io.quarkus.it.opentelemetry;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import io.restassured.common.mapper.TypeRef;

public abstract class OpenTelemetryJdbcInstrumentationTest {

    @BeforeEach
    @AfterEach
    void reset() {
        given().get("/reset").then().statusCode(HTTP_OK);
        await().atMost(30, SECONDS).until(() -> {
            // make sure spans are cleared
            List<Map<String, Object>> spans = getSpans();
            if (spans.size() > 0) {
                given().get("/reset").then().statusCode(HTTP_OK);
            }
            return spans.size() == 0;
        });
    }

    private List<Map<String, Object>> getSpans() {
        return get("/export").body().as(new TypeRef<>() {
        });
    }

    protected void testQueryTraced(String dbKind, String expectedTable) {
        given()
                .queryParam("id", 1)
                .when().post("/hit/" + dbKind)
                .then()
                .statusCode(200)
                .body("message", Matchers.equalTo("Hit message."));

        Awaitility.await().atMost(Duration.ofSeconds(55)).untilAsserted(() -> {
            assertFalse(getSpans().isEmpty());

            // Assert insert has been traced
            boolean hitInserted = false;
            for (Map<String, Object> spanData : getSpans()) {
                if (spanData.get("attributes") instanceof Map) {
                    final Map attributes = (Map) spanData.get("attributes");
                    var dbOperation = attributes.get("db.operation");
                    var dbTable = attributes.get("db.sql.table");
                    if ("INSERT".equals(dbOperation) && expectedTable.equals(dbTable)) {
                        hitInserted = true;
                        break;
                    }
                }
            }
            assertTrue(hitInserted, "JDBC insert statement was not traced.");
        });
    }

    protected void testQueryNotTraced(String dbKind) {
        given()
                .queryParam("id", 1)
                .when().post("/hit/" + dbKind)
                .then()
                .statusCode(200)
                .body("message", Matchers.equalTo("Hit message."));

        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            assertTrue(getSpans().isEmpty(), "No spans should be recorded when OpenTelemetry is disabled.");
        });
    }
}
