package io.quarkus.it.opentelemetry;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(OpenTelemetryDisabledTest.MyProfile.class)
public class OpenTelemetryDisabledTest extends BaseTest {

    @Test
    void buildTimeDisabled() {
        given()
                .when().get("/hello")
                .then()
                .statusCode(200)
                .body(is("Hello from RESTEasy Reactive"));
        // Service will start nevertheless.
        await().atMost(200, MILLISECONDS).until(() -> getSpans().size() == 0);
    }

    public static class MyProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.otel.enabled", "false");
        }
    }
}
