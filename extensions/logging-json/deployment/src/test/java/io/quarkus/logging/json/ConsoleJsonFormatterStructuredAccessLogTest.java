package io.quarkus.logging.json;

import static io.quarkus.logging.json.ConsoleJsonFormatterDefaultConfigTest.getJsonFormatter;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;

import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.json.runtime.JsonFormatter;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Tests for the {@code quarkus.log.console.json.access-log.structured} option.
 * <p>
 * Access log records are identified by a logger name that starts with
 * {@code io.quarkus.http.access-log}. When the option is enabled, structured
 * MDC fields placed by {@code JBossLoggingAccessLogReceiver} under the
 * {@code __access__} prefix must be written as a nested {@code "accessLog"} JSON
 * object, and must NOT appear inside the regular {@code "mdc"} object.
 */
public class ConsoleJsonFormatterStructuredAccessLogTest {

    private static final String ACCESS_LOG_LOGGER = "io.quarkus.http.access-log";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(ConsoleJsonFormatterDefaultConfigTest.class))
            .withConfigurationResource("application-console-json-formatter-structured-access-log.properties");

    @Test
    public void structuredAccessLogEnabledConfigTest() {
        JsonFormatter jsonFormatter = getJsonFormatter();
        assertThat(jsonFormatter.isStructuredAccessLog()).isTrue();
    }

    @Test
    public void accessLogRecordProducesAccessLogObjectTest() throws Exception {
        JsonFormatter formatter = new JsonFormatter();
        formatter.setStructuredAccessLog(true);

        ExtLogRecord record = new ExtLogRecord(Level.INFO, "GET /api/health 200", ACCESS_LOG_LOGGER);
        record.putMdc("__access__method", "GET");
        record.putMdc("__access__uri", "/api/health");
        record.putMdc("__access__status", "200");
        record.putMdc("__access__responseTimeMs", "12");
        record.putMdc("__access__bytesSent", "512");
        record.putMdc("__access__remoteIp", "10.0.0.1");
        record.putMdc("__access__protocol", "HTTP/1.1");

        String line = formatter.format(record);
        JsonNode node = MAPPER.readTree(line);

        assertThat(node.has("accessLog")).isTrue();
        JsonNode accessLog = node.get("accessLog");

        assertThat(accessLog.get("method").asText()).isEqualTo("GET");
        assertThat(accessLog.get("uri").asText()).isEqualTo("/api/health");
        assertThat(accessLog.get("protocol").asText()).isEqualTo("HTTP/1.1");
        assertThat(accessLog.get("remoteIp").asText()).isEqualTo("10.0.0.1");

        // Numeric fields must be emitted as JSON numbers, not strings
        assertThat(accessLog.get("status").isNumber()).isTrue();
        assertThat(accessLog.get("status").asInt()).isEqualTo(200);
        assertThat(accessLog.get("responseTimeMs").isNumber()).isTrue();
        assertThat(accessLog.get("responseTimeMs").asLong()).isEqualTo(12L);
        assertThat(accessLog.get("bytesSent").isNumber()).isTrue();
        assertThat(accessLog.get("bytesSent").asLong()).isEqualTo(512L);

        // The __access__* keys must NOT appear in the mdc object
        if (node.has("mdc")) {
            JsonNode mdc = node.get("mdc");
            mdc.fieldNames().forEachRemaining(key -> assertThat(key).doesNotStartWith("__access__"));
        }
    }

    @Test
    public void accessLogFieldsNotPresentInMdcObjectTest() throws Exception {
        JsonFormatter formatter = new JsonFormatter();
        formatter.setStructuredAccessLog(true);

        ExtLogRecord record = new ExtLogRecord(Level.INFO, "POST /api/v1/orders 201", ACCESS_LOG_LOGGER);
        record.putMdc("__access__method", "POST");
        record.putMdc("__access__status", "201");
        record.putMdc("requestId", "req-abc");

        String line = formatter.format(record);
        JsonNode node = MAPPER.readTree(line);

        assertThat(node.has("accessLog")).isTrue();
        JsonNode mdc = node.get("mdc");
        if (mdc != null) {
            assertThat(mdc.has("__access__method")).isFalse();
            assertThat(mdc.has("__access__status")).isFalse();
            assertThat(mdc.has("requestId")).isTrue();
            assertThat(mdc.get("requestId").asText()).isEqualTo("req-abc");
        }
    }

    @Test
    public void nonAccessLogRecordUnaffectedTest() throws Exception {
        JsonFormatter formatter = new JsonFormatter();
        formatter.setStructuredAccessLog(true);

        ExtLogRecord record = new ExtLogRecord(Level.INFO, "application event",
                "com.example.MyService");

        String line = formatter.format(record);
        JsonNode node = MAPPER.readTree(line);

        assertThat(node.has("accessLog")).isFalse();
        assertThat(node.get("message").asText()).isEqualTo("application event");
    }

    @Test
    public void disabledByDefaultLeavesAccessLogAsPlainMessageTest() throws Exception {
        JsonFormatter formatter = new JsonFormatter();
        assertThat(formatter.isStructuredAccessLog()).isFalse();

        ExtLogRecord record = new ExtLogRecord(Level.INFO, "GET /ping 200", ACCESS_LOG_LOGGER);
        record.putMdc("__access__method", "GET");
        record.putMdc("__access__status", "200");

        String line = formatter.format(record);
        JsonNode node = MAPPER.readTree(line);

        assertThat(node.has("accessLog")).isFalse();
        assertThat(node.get("message").asText()).isEqualTo("GET /ping 200");
        assertThat(node.has("mdc")).isTrue();
    }

    @Test
    public void accessLogObjectAbsentWhenNoMdcFieldsPresentTest() throws Exception {
        JsonFormatter formatter = new JsonFormatter();
        formatter.setStructuredAccessLog(true);

        ExtLogRecord record = new ExtLogRecord(Level.INFO, "DELETE /item/1 204", ACCESS_LOG_LOGGER);

        String line = formatter.format(record);
        JsonNode node = MAPPER.readTree(line);

        assertThat(node.has("accessLog")).isFalse();
    }

    @Test
    public void mdcObjectAbsentWhenOnlyAccessPrefixedKeysArePresentTest() throws Exception {
        // When the MDC contains ONLY __access__* keys (no user MDC entries), the "mdc"
        // object must be entirely absent from the output — not emitted as an empty object.
        JsonFormatter formatter = new JsonFormatter();
        formatter.setStructuredAccessLog(true);

        ExtLogRecord record = new ExtLogRecord(Level.INFO, "GET /health 200", ACCESS_LOG_LOGGER);
        record.putMdc("__access__method", "GET");
        record.putMdc("__access__uri", "/health");
        record.putMdc("__access__status", "200");

        String line = formatter.format(record);
        JsonNode node = MAPPER.readTree(line);

        assertThat(node.has("accessLog")).isTrue();
        // All MDC entries were __access__-prefixed; nothing should remain in the "mdc" object.
        assertThat(node.has("mdc")).isFalse();
    }

    @Test
    public void flatMdcAndStructuredAccessLogCombinedTest() throws Exception {
        JsonFormatter formatter = new JsonFormatter();
        formatter.setStructuredAccessLog(true);
        formatter.setFlatMdc(true);

        ExtLogRecord record = new ExtLogRecord(Level.INFO, "PUT /resource 204", ACCESS_LOG_LOGGER);
        record.putMdc("__access__method", "PUT");
        record.putMdc("__access__status", "204");
        record.putMdc("traceId", "trace-xyz");

        String line = formatter.format(record);
        JsonNode node = MAPPER.readTree(line);

        assertThat(node.has("traceId")).isTrue();
        assertThat(node.get("traceId").asText()).isEqualTo("trace-xyz");

        assertThat(node.has("__access__method")).isFalse();
        assertThat(node.has("__access__status")).isFalse();
        assertThat(node.has("accessLog")).isTrue();
        assertThat(node.get("accessLog").get("method").asText()).isEqualTo("PUT");
    }
}
