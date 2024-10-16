package io.quarkus.it.opentelemetry;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.SpanKind;
import io.quarkus.it.opentelemetry.profile.RedisInstrumentationDisabledProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
@TestProfile(RedisInstrumentationDisabledProfile.class)
public class QuarkusOpenTelemetryRedisDisabledTest {

    @BeforeEach
    void reset() {
        given().get("/opentelemetry/reset").then().statusCode(HTTP_OK);
        await().atMost(5, SECONDS).until(() -> getSpans().isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void operationIsNotTraced() {
        String path = "redis/sync/disabledKey";
        RestAssured.given()
                .body("disabledValue")
                .when()
                .post(path)
                .then()
                .statusCode(204);

        Awaitility.await().atMost(Duration.ofSeconds(10)).until(() -> getSpans().size() == 1);

        List<Map<String, Object>> spans = getSpans();
        Map<String, Object> span = spans.get(0);

        // SET
        assertEquals(1, spans.size());
        assertEquals("POST /redis/sync/{key}", span.get("name"));
        assertEquals(SpanKind.SERVER.name(), span.get("kind"));
    }

    private List<Map<String, Object>> getSpans() {
        return get("/opentelemetry/export").body().as(new TypeRef<>() {
        });
    }
}
