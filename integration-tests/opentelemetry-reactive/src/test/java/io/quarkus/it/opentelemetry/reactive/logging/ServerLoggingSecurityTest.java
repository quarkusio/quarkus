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
@TestProfile(ServerLoggingSecurityTestProfile.class)
public class ServerLoggingSecurityTest {

    @BeforeAll
    static void installLogCapture() {
        LogCapture.install();
    }

    @BeforeEach
    void clearRecords() {
        LogCapture.records.clear();
    }

    @Test
    void testUnauthenticatedRequestOnlyResponseLogged() {
        // The Quarkus security filter runs at Priorities.AUTHENTICATION (1000), which aborts
        // before our logging filter at Priorities.USER (5000) — the request is never logged.
        // The response filter still runs because JAX-RS invokes response filters at priority
        // >= the aborting filter's priority, so the 401 response IS logged.
        RestAssured.get("/log-test/secured")
                .then()
                .statusCode(401);

        List<String> messages = LogCapture.records.stream().map(LogRecord::getMessage).collect(Collectors.toList());
        assertThat(messages).noneMatch(m -> m.startsWith("Request:") && m.contains("/log-test/secured"));
        assertThat(messages).anyMatch(m -> m.startsWith("Response: GET") && m.contains("/log-test/secured")
                && m.contains("Status[401 Unauthorized]")
                && m.contains("Headers[")
                && m.contains("www-authenticate="));
    }

    @Test
    void testAuthenticatedRequestLogged() {
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
