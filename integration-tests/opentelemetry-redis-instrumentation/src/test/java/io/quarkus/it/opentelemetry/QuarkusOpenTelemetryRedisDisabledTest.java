package io.quarkus.it.opentelemetry;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.it.opentelemetry.profile.RedisInstrumentationDisabledProfile;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.opentelemetry.SpanDataList;
import io.quarkus.test.opentelemetry.SpanExporterClient;
import io.restassured.RestAssured;

@QuarkusTest
@TestProfile(RedisInstrumentationDisabledProfile.class)
public class QuarkusOpenTelemetryRedisDisabledTest {

    @TestHTTPResource
    URI applicationUri;

    SpanExporterClient spanExporter() {
        return SpanExporterClient.at(applicationUri);
    }

    @BeforeEach
    void reset() {
        spanExporter().reset();
    }

    @Test
    public void operationIsNotTraced() {
        String path = "redis/sync/disabledKey";
        RestAssured.given()
                .body("disabledValue")
                .when()
                .post(path)
                .then()
                .statusCode(204);

        SpanDataList spans = spanExporter().awaitCount(1, Duration.ofSeconds(10));
        SpanData span = spans.single();

        // SET
        assertThat(span)
                .hasName("POST /redis/sync/{key}")
                .hasKind(SpanKind.SERVER);
    }
}
