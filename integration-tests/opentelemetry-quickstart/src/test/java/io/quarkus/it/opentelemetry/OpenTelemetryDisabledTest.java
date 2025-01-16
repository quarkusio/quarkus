package io.quarkus.it.opentelemetry;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(OpenTelemetryDisabledTest.MyProfile.class)
public class OpenTelemetryDisabledTest extends BaseTest {

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
    void buildTimeDisabled() {
        given()
                .when().get("/hello")
                .then()
                .statusCode(200)
                .body(is("Hello from Quarkus REST"));
        // Service will start nevertheless.
        await().atMost(300, MILLISECONDS).until(() -> getSpans().size() == 0);
    }

    public static class MyProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.otel.enabled", "false");
        }
    }
}
