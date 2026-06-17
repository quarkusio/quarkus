package io.quarkus.logging.json;

import static io.quarkus.logging.json.ConsoleJsonFormatterDefaultConfigTest.getJsonFormatter;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.LogRecord;

import org.assertj.core.api.Assertions;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.formatters.StructuredFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.json.runtime.JsonFormatter;
import io.quarkus.test.QuarkusExtensionTest;

public class ConsoleJsonFormatterECSConfigTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(ConsoleJsonFormatterDefaultConfigTest.class))
            .withConfigurationResource("application-console-json-formatter-ecs.properties");

    @Test
    public void ecsFormatterConfigurationTest() {
        JsonFormatter jsonFormatter = getJsonFormatter();
        // When ECS format is active the formatter must use FORMATTED output type so that
        // the stack trace is written as a flat string to error.stack_trace and the
        // structured "exception" object is never emitted.
        assertThat(jsonFormatter.getExceptionOutputType())
                .isEqualTo(StructuredFormatter.ExceptionOutputType.FORMATTED);
    }

    @Test
    public void ecsFormatterOutputWithExceptionTest() throws Exception {
        JsonFormatter jsonFormatter = getJsonFormatter();

        // Build a log record carrying a thrown exception
        ExtLogRecord record = new ExtLogRecord(Level.ERROR, "application failed", "test.Logger");
        record.setThrown(new IllegalStateException("This is a startup exception"));

        String output = jsonFormatter.format(record);
        JsonNode node = new ObjectMapper().readTree(output);

        // error.stack_trace must be a plain text string
        Assertions.assertThat(node.has("error.stack_trace"))
                .as("error.stack_trace must be present for ECS format")
                .isTrue();
        Assertions.assertThat(node.get("error.stack_trace").isTextual())
                .as("error.stack_trace must be a string, not an object")
                .isTrue();
        Assertions.assertThat(node.get("error.stack_trace").asText())
                .doesNotStartWith(": ")
                .contains("IllegalStateException")
                .contains("This is a startup exception");

        // error.message must be a top-level ECS field with the exception message
        Assertions.assertThat(node.has("error.message"))
                .as("error.message must be present as a top-level field")
                .isTrue();
        Assertions.assertThat(node.get("error.message").asText())
                .isEqualTo("This is a startup exception");

        // error.type must be a top-level ECS field with the exception class name
        Assertions.assertThat(node.has("error.type"))
                .as("error.type must be present as a top-level field")
                .isTrue();
        Assertions.assertThat(node.get("error.type").asText())
                .isEqualTo(IllegalStateException.class.getName());

        // The structured "exception" object must NOT appear in ECS output
        Assertions.assertThat(node.has("exception"))
                .as("structured 'exception' object must not appear in ECS output")
                .isFalse();

        // Standard ECS fields must be present
        Assertions.assertThat(node.has("@timestamp")).isTrue();
        Assertions.assertThat(node.has("log.level")).isTrue();
        Assertions.assertThat(node.has("log.logger")).isTrue();
        Assertions.assertThat(node.has("ecs.version")).isTrue();

        // mdc and ndc are not ECS fields and must be absent
        Assertions.assertThat(node.has("mdc"))
                .as("mdc is not an ECS field and must be excluded")
                .isFalse();
        Assertions.assertThat(node.has("ndc"))
                .as("ndc is not an ECS field and must be excluded")
                .isFalse();

        // process.name must be the short executable name, not the full JVM path
        if (node.has("process.name")) {
            Assertions.assertThat(node.get("process.name").asText())
                    .as("process.name must be a basename, not a full path")
                    .doesNotContain("/");
        }
    }

    @Test
    public void ecsFormatterOutputWithoutExceptionTest() throws Exception {
        JsonFormatter jsonFormatter = getJsonFormatter();

        // A normal log record without a thrown exception must not produce any error fields
        LogRecord record = new LogRecord(Level.INFO, "Hello ECS");
        String output = jsonFormatter.format(record);
        JsonNode node = new ObjectMapper().readTree(output);

        Assertions.assertThat(node.has("error.stack_trace")).isFalse();
        Assertions.assertThat(node.has("exception")).isFalse();
        Assertions.assertThat(node.get("message").asText()).isEqualTo("Hello ECS");
        // mdc and ndc must also be absent for non-exception records
        Assertions.assertThat(node.has("mdc")).isFalse();
        Assertions.assertThat(node.has("ndc")).isFalse();
    }
}
