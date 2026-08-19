package io.quarkus.logging.json;

import static io.quarkus.logging.json.ConsoleJsonFormatterDefaultConfigTest.getJsonFormatter;
import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.json.runtime.JsonFormatter;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * MDC data must survive the ECS log format. ECS does not define an {@code mdc} field, but it does
 * allow custom fields, and dropping the data altogether loses information the application put
 * there deliberately — including the OpenTelemetry trace identifiers.
 * See <a href="https://github.com/quarkusio/quarkus/issues/56021">GitHub issue #56021</a>.
 */
public class ConsoleJsonFormatterEcsMdcTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(ConsoleJsonFormatterDefaultConfigTest.class))
            .withConfigurationResource("application-console-json-formatter-ecs.properties");

    @Test
    public void mdcIsRetainedInEcsFormat() throws Exception {
        JsonFormatter jsonFormatter = getJsonFormatter();

        ExtLogRecord record = new ExtLogRecord(Level.INFO, "ecs mdc test", ConsoleJsonFormatterEcsMdcTest.class.getName());
        record.putMdc("requestId", "abc123");
        record.putMdc("traceId", "0af7651916cd43dd8448eb211c80319c");

        JsonNode node = new ObjectMapper().readTree(jsonFormatter.format(record));

        assertThat(node.has("mdc"))
                .as("MDC data must not be dropped from ECS output")
                .isTrue();
        assertThat(node.get("mdc").get("requestId").asText()).isEqualTo("abc123");
        assertThat(node.get("mdc").get("traceId").asText()).isEqualTo("0af7651916cd43dd8448eb211c80319c");

        // The ECS field mapping must still apply
        assertThat(node.has("@timestamp")).isTrue();
        assertThat(node.has("log.level")).isTrue();
    }

    @Test
    public void mdcIsRetainedAsFlatFieldsInEcsFormat() throws Exception {
        JsonFormatter jsonFormatter = getJsonFormatter();
        boolean originalFlatMdc = jsonFormatter.isFlatMdc();
        jsonFormatter.setFlatMdc(true);
        try {
            ExtLogRecord record = new ExtLogRecord(Level.INFO, "ecs flat mdc test",
                    ConsoleJsonFormatterEcsMdcTest.class.getName());
            record.putMdc("requestId", "abc123");

            JsonNode node = new ObjectMapper().readTree(jsonFormatter.format(record));

            assertThat(node.has("mdc"))
                    .as("with flat fields the nested mdc object must not be written")
                    .isFalse();
            assertThat(node.has("requestId"))
                    .as("MDC entries must be written as root-level fields in ECS output")
                    .isTrue();
            assertThat(node.get("requestId").asText()).isEqualTo("abc123");
        } finally {
            jsonFormatter.setFlatMdc(originalFlatMdc);
        }
    }

    @Test
    public void ndcIsRetainedInEcsFormat() throws Exception {
        JsonFormatter jsonFormatter = getJsonFormatter();

        ExtLogRecord record = new ExtLogRecord(Level.INFO, "ecs ndc test", ConsoleJsonFormatterEcsMdcTest.class.getName());
        record.setNdc("some-nested-context");

        JsonNode node = new ObjectMapper().readTree(jsonFormatter.format(record));

        assertThat(node.has("ndc"))
                .as("NDC data must not be dropped from ECS output")
                .isTrue();
        assertThat(node.get("ndc").asText()).isEqualTo("some-nested-context");
    }
}
