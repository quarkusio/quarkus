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

public class ConsoleJsonFormatterFlatMdcTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(ConsoleJsonFormatterDefaultConfigTest.class))
            .withConfigurationResource("application-console-json-formatter-flat-mdc.properties");

    @Test
    public void flatMdcEnabledConfigTest() {
        JsonFormatter jsonFormatter = getJsonFormatter();
        assertThat(jsonFormatter.isFlatMdc()).isTrue();
    }

    @Test
    public void flatMdcFieldsAtRootLevelTest() throws Exception {
        JsonFormatter jsonFormatter = getJsonFormatter();

        ExtLogRecord record = new ExtLogRecord(Level.INFO, "flat MDC test", ConsoleJsonFormatterFlatMdcTest.class.getName());
        record.putMdc("requestId", "abc123");
        record.putMdc("userId", "42");

        String line = jsonFormatter.format(record);
        JsonNode node = new ObjectMapper().readTree(line);

        // MDC wrapper object must not be present
        assertThat(node.has("mdc")).isFalse();

        // Each MDC entry must be a root-level field
        assertThat(node.has("requestId")).isTrue();
        assertThat(node.get("requestId").asText()).isEqualTo("abc123");
        assertThat(node.has("userId")).isTrue();
        assertThat(node.get("userId").asText()).isEqualTo("42");

        // Standard fields are still present
        assertThat(node.has("message")).isTrue();
        assertThat(node.get("message").asText()).isEqualTo("flat MDC test");
    }

    @Test
    public void flatMdcEmptyMdcProducesNoExtraFieldsTest() throws Exception {
        // Create the formatter directly so this test is self-contained.
        JsonFormatter formatter = new JsonFormatter();
        formatter.setFlatMdc(true);

        // An ExtLogRecord with no putMdc() calls; the MDC map comes from the
        // thread-local MDC which is empty in this test context.
        ExtLogRecord record = new ExtLogRecord(Level.INFO, "no MDC",
                ConsoleJsonFormatterFlatMdcTest.class.getName());

        String line = formatter.format(record);
        JsonNode node = new ObjectMapper().readTree(line);

        // With flat-mdc=true and an empty MDC map, neither the "mdc" wrapper
        // nor any flat MDC fields should appear.
        assertThat(node.has("mdc")).isFalse();
        assertThat(node.get("message").asText()).isEqualTo("no MDC");
    }

    @Test
    public void defaultBehaviorKeepsMdcNestedTest() throws Exception {
        // Regression: a formatter without flat-mdc enabled must still produce nested MDC.
        JsonFormatter formatter = new JsonFormatter();
        assertThat(formatter.isFlatMdc()).isFalse();

        ExtLogRecord record = new ExtLogRecord(Level.INFO, "nested MDC test",
                ConsoleJsonFormatterFlatMdcTest.class.getName());
        record.putMdc("traceId", "xyz789");

        String line = formatter.format(record);
        JsonNode node = new ObjectMapper().readTree(line);

        assertThat(node.has("mdc")).isTrue();
        assertThat(node.get("mdc").get("traceId").asText()).isEqualTo("xyz789");
    }
}
