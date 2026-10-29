package io.quarkus.logging.json;

import static io.quarkus.logging.json.ConsoleJsonFormatterDefaultConfigTest.getJsonFormatter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.OffsetDateTime;
import java.util.logging.LogRecord;

import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.formatters.StructuredFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.logging.json.runtime.JsonFormatter;
import io.quarkus.logging.json.runtime.JsonLogConfig.AdditionalFieldConfig;
import io.quarkus.test.QuarkusExtensionTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class ConsoleJsonFormatterLogstashConfigTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(ConsoleJsonFormatterDefaultConfigTest.class))
            .overrideConfigKey("quarkus.log.level", "INFO")
            .overrideConfigKey("quarkus.log.console.enabled", "true")
            .overrideConfigKey("quarkus.log.console.level", "WARNING")
            .overrideConfigKey("quarkus.log.console.format", "%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n")
            .overrideConfigKey("quarkus.log.console.json.enabled", "true")
            .overrideConfigKey("quarkus.log.console.json.log-format", "logstash");

    @Test
    public void logstashFormatterConfigurationTest() {
        JsonFormatter jsonFormatter = getJsonFormatter();
        assertThat(jsonFormatter.getExceptionOutputType())
                .isEqualTo(StructuredFormatter.ExceptionOutputType.FORMATTED);
        assertThat(jsonFormatter.isFlatMdc())
                .as("MDC entries are root-level fields in the Logstash format")
                .isTrue();
        assertThat(jsonFormatter.getKeyOverrides())
                .contains("TIMESTAMP=@timestamp", "LOGGER_NAME=logger_name", "THREAD_NAME=thread_name",
                        "STACK_TRACE=stack_trace");
        assertThat(jsonFormatter.getAdditionalFields().get("@version").value()).isEqualTo("1");
        assertThat(jsonFormatter.getAdditionalFields().get("@version").type())
                .isEqualTo(AdditionalFieldConfig.Type.STRING);
        assertThat(jsonFormatter.getExcludedKeys())
                .contains("sequence", "loggerClassName", "hostName", "processId", "processName", "threadId", "ndc");
    }

    @Test
    public void logstashFormatterOutputTest() throws Exception {
        JsonFormatter jsonFormatter = getJsonFormatter();

        ExtLogRecord record = new ExtLogRecord(Level.ERROR, "application failed", "test.Logger");
        record.setLoggerName("test.Logger");
        record.setThrown(new IllegalStateException("This is a startup exception"));

        JsonNode node = new ObjectMapper().readTree(jsonFormatter.format(record));

        assertThat(node.get("@timestamp").asText()).isNotBlank();
        assertThatCode(() -> OffsetDateTime.parse(node.get("@timestamp").asText())).doesNotThrowAnyException();
        assertThat(node.get("@version").asText()).isEqualTo("1");
        assertThat(node.get("message").asText()).isEqualTo("application failed");
        assertThat(node.get("logger_name").asText()).isEqualTo("test.Logger");
        assertThat(node.has("thread_name")).isTrue();
        assertThat(node.get("level").asText()).isEqualTo("ERROR");
        assertThat(node.get("level_value").asInt()).isEqualTo(40_000);

        assertThat(node.get("stack_trace").isTextual())
                .as("stack_trace must be a string, not an object")
                .isTrue();
        assertThat(node.get("stack_trace").asText())
                .doesNotStartWith(": ")
                .contains("IllegalStateException")
                .contains("This is a startup exception");

        assertThat(node.has("timestamp")).isFalse();
        assertThat(node.has("loggerName")).isFalse();
        assertThat(node.has("threadName")).isFalse();
        assertThat(node.has("sequence")).isFalse();
        assertThat(node.has("loggerClassName")).isFalse();
        assertThat(node.has("hostName")).isFalse();
        assertThat(node.has("processName")).isFalse();
        assertThat(node.has("processId")).isFalse();
        assertThat(node.has("threadId")).isFalse();
        assertThat(node.has("ndc")).isFalse();
        assertThat(node.has("mdc")).isFalse();
        assertThat(node.has("exception")).isFalse();
    }

    @Test
    public void logstashFormatterOutputWithoutExceptionTest() throws Exception {
        JsonFormatter jsonFormatter = getJsonFormatter();

        LogRecord record = new LogRecord(Level.INFO, "Hello Logstash");
        JsonNode node = new ObjectMapper().readTree(jsonFormatter.format(record));

        assertThat(node.has("stack_trace")).isFalse();
        assertThat(node.get("message").asText()).isEqualTo("Hello Logstash");
        assertThat(node.get("level").asText()).isEqualTo("INFO");
        assertThat(node.get("level_value").asInt()).isEqualTo(20_000);
    }

    @Test
    public void logstashMdcIsWrittenAsRootLevelFieldsTest() throws Exception {
        JsonFormatter jsonFormatter = getJsonFormatter();

        ExtLogRecord record = new ExtLogRecord(Level.INFO, "logstash mdc test", "test.Logger");
        record.putMdc("requestId", "abc123");
        record.setNdc("nested-context");

        JsonNode node = new ObjectMapper().readTree(jsonFormatter.format(record));

        assertThat(node.has("mdc"))
                .as("MDC entries must be root-level fields in Logstash output")
                .isFalse();
        assertThat(node.get("requestId").asText()).isEqualTo("abc123");
        assertThat(node.has("ndc")).isFalse();
    }

    @Test
    public void logstashLevelValueMappingTest() throws Exception {
        JsonFormatter jsonFormatter = getJsonFormatter();
        ObjectMapper mapper = new ObjectMapper();

        assertThat(levelValue(mapper, jsonFormatter, new LogRecord(Level.SEVERE, "level value test"))).isEqualTo(40_000);
        assertThat(levelValue(mapper, jsonFormatter, new LogRecord(Level.ERROR, "level value test"))).isEqualTo(40_000);
        assertThat(levelValue(mapper, jsonFormatter, new LogRecord(Level.FATAL, "level value test"))).isEqualTo(40_000);
        assertThat(levelValue(mapper, jsonFormatter, new LogRecord(Level.WARNING, "level value test"))).isEqualTo(30_000);
        assertThat(levelValue(mapper, jsonFormatter, new LogRecord(Level.WARN, "level value test"))).isEqualTo(30_000);
        assertThat(levelValue(mapper, jsonFormatter, new LogRecord(Level.INFO, "level value test"))).isEqualTo(20_000);
        assertThat(levelValue(mapper, jsonFormatter, new LogRecord(Level.CONFIG, "level value test"))).isEqualTo(10_000);
        assertThat(levelValue(mapper, jsonFormatter, new LogRecord(Level.FINE, "level value test"))).isEqualTo(10_000);
        assertThat(levelValue(mapper, jsonFormatter, new LogRecord(Level.DEBUG, "level value test"))).isEqualTo(10_000);
        assertThat(levelValue(mapper, jsonFormatter, new LogRecord(Level.FINER, "level value test"))).isEqualTo(5_000);
        assertThat(levelValue(mapper, jsonFormatter, new LogRecord(Level.FINEST, "level value test"))).isEqualTo(5_000);
        assertThat(levelValue(mapper, jsonFormatter, new LogRecord(Level.TRACE, "level value test"))).isEqualTo(5_000);
    }

    private static int levelValue(ObjectMapper mapper, JsonFormatter formatter, LogRecord record) throws Exception {
        return mapper.readTree(formatter.format(record)).get("level_value").asInt();
    }
}
