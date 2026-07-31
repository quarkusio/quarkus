package io.quarkus.it.logging.json;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

/**
 * Integration test for the {@code access-log.structured} JSON logging feature.
 * <p>
 * Boots a real Quarkus application with HTTP access logging enabled and
 * {@code quarkus.log.file.json.access-log.structured=true}, fires a real
 * HTTP request, then asserts that the resulting JSON log line contains a
 * nested {@code accessLog} object with the expected structured fields.
 */
@QuarkusTest
@TestProfile(StructuredAccessLogJsonTest.StructuredAccessLogProfile.class)
public class StructuredAccessLogJsonTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static final Path LOG_FILE = Path.of(
            System.getProperty("java.io.tmpdir"), "quarkus-sal-it-test.json");

    @Test
    public void accessLogRecordContainsStructuredAccessLogObject() throws Exception {
        Files.writeString(LOG_FILE, "");

        given().get("/hello").then().statusCode(200);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<String> lines = Files.readAllLines(LOG_FILE);
            Optional<JsonNode> accessLine = lines.stream()
                    .filter(l -> !l.isBlank())
                    .map(l -> {
                        try {
                            return MAPPER.readTree(l);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(node -> node != null
                            && node.path("loggerName").asText("").startsWith("io.quarkus.http.access-log"))
                    .findFirst();

            assertThat(accessLine).as("access-log JSON line must be present in %s", LOG_FILE).isPresent();
            JsonNode node = accessLine.get();

            assertThat(node.has("accessLog")).as("accessLog object must be present").isTrue();
            JsonNode al = node.get("accessLog");

            assertThat(al.path("method").asText()).isEqualTo("GET");
            assertThat(al.path("uri").asText()).isEqualTo("/hello");
            assertThat(al.path("status").asInt()).isEqualTo(200);
            assertThat(al.path("responseTimeMs").isNumber()).isTrue();
            assertThat(al.path("responseTimeMs").asLong()).isGreaterThanOrEqualTo(0);
            assertThat(al.path("bytesSent").isNumber()).isTrue();
            assertThat(al.path("remoteIp").asText()).isNotBlank();
            assertThat(al.path("protocol").asText()).isNotBlank();

            if (node.has("mdc")) {
                node.get("mdc").fieldNames()
                        .forEachRemaining(k -> assertThat(k).doesNotStartWith("__access__"));
            }
        });
    }

    public static class StructuredAccessLogProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            String logFile = System.getProperty("java.io.tmpdir") + "/quarkus-sal-it-test.json";
            return Map.of(
                    "quarkus.http.access-log.enabled", "true",
                    "quarkus.log.file.enabled", "true",
                    "quarkus.log.file.path", logFile,
                    "quarkus.log.file.json.enabled", "true",
                    "quarkus.log.file.json.access-log.structured", "true");
        }
    }
}
