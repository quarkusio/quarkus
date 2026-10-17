package io.quarkus.it.opentelemetry;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.it.opentelemetry.util.AccessLogProfile;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;

/**
 * Verifies that OTel traceId and spanId are available in the access log
 * for reactive endpoints running on the Vert.x event loop.
 * <p>
 * This must be an integration test because the bug only manifests in prod mode
 * where the OTel scope closes before the access log handler reads the MDC.
 */
@QuarkusIntegrationTest
@TestProfile(AccessLogProfile.class)
public class AccessLogTracingIT {

    @Test
    void traceIdAndSpanIdInAccessLog() throws Exception {
        given()
                .contentType("application/json")
                .when().get("/access-log-test/async")
                .then()
                .statusCode(200);

        // Give time for the access log to be flushed
        Thread.sleep(1000);

        // QuarkusIntegrationTest captures app output to target/test-*/quarkus.log
        Path targetDir = Path.of("target");
        List<Path> logFiles = Files.list(targetDir)
                .filter(p -> p.getFileName().toString().startsWith("test-"))
                .flatMap(p -> {
                    try {
                        return Files.list(p);
                    } catch (Exception e) {
                        return java.util.stream.Stream.empty();
                    }
                })
                .filter(p -> p.getFileName().toString().equals("quarkus.log"))
                .toList();

        assertFalse(logFiles.isEmpty(), "Should find quarkus.log in target/test-*/");

        String logs = Files.readString(logFiles.get(0));
        assertFalse(logs.contains("traceId=-"),
                "Access log must not show traceId=- for traced requests.\n" + logs);
    }
}
