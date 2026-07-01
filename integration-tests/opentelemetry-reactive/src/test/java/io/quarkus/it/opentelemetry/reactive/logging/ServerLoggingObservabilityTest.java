package io.quarkus.it.opentelemetry.reactive.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

@QuarkusTest
@TestProfile(ServerLoggingObservabilityTestProfile.class)
public class ServerLoggingObservabilityTest {

    @BeforeAll
    static void installLogCapture() {
        LogCapture.install();
    }

    @BeforeEach
    void clearRecords() {
        LogCapture.records.clear();
    }

    @Test
    void testLoggingWithOtelAndMetricsActive() {
        RestAssured.given()
                .auth().preemptive().basic("alice", "alice")
                .get("/log-test/secured")
                .then()
                .statusCode(200);

        List<String> messages = LogCapture.records.stream().map(LogRecord::getMessage).collect(Collectors.toList());
        assertThat(messages).hasSize(2);
        assertThat(messages).anyMatch(m -> m.startsWith("Request: GET") && m.contains("/log-test/secured")
                && m.contains("Headers["));
        assertThat(messages).anyMatch(m -> m.startsWith("Response: GET") && m.contains("/log-test/secured")
                && m.contains("Status[200 OK]")
                && m.contains("Headers["));

        String metrics = RestAssured.get("/q/metrics").then().statusCode(200).extract().body().asString();
        assertThat(metrics).contains("http_server_requests_seconds_count")
                .contains("uri=\"/log-test/secured\"");
    }

    @Test
    void testAccessLogIsIndependentFromRestLogging() {
        RestAssured.given()
                .auth().preemptive().basic("alice", "alice")
                .get("/log-test/secured")
                .then()
                .statusCode(200);

        List<String> messages = LogCapture.records.stream().map(LogRecord::getMessage).collect(Collectors.toList());
        assertThat(messages).hasSize(2);
        assertThat(messages).anyMatch(m -> m.startsWith("Request: GET") && m.contains("/log-test/secured")
                && m.contains("Headers["));
        assertThat(messages).anyMatch(m -> m.startsWith("Response: GET") && m.contains("/log-test/secured")
                && m.contains("Status[200 OK]")
                && m.contains("Headers["));
    }
}
