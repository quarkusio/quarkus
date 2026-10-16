package io.quarkus.logging.json;

import static io.quarkus.logging.json.ConsoleJsonFormatterDefaultConfigTest.getJsonFormatter;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.logging.Level;

import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.logging.json.runtime.JsonFormatter;
import io.quarkus.test.QuarkusExtensionTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class ConsoleJsonFormatterMdcKeyOverridesTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(ConsoleJsonFormatterDefaultConfigTest.class))
            .withConfigurationResource("application-console-json-formatter-mdc-key-overrides.properties");

    @Test
    public void mdcKeyOverridesConfigTest() {
        JsonFormatter jsonFormatter = getJsonFormatter();
        assertThat(jsonFormatter.getMdcKeyOverrides())
                .containsExactlyInAnyOrderEntriesOf(Map.of("traceId", "TraceId", "spanId", "SpanId"));
    }

    @Test
    public void nestedMdcKeysAreRenamedTest() throws Exception {
        JsonFormatter jsonFormatter = getJsonFormatter();

        String line = jsonFormatter.format(recordWithTraceMdc());
        JsonNode mdc = new ObjectMapper().readTree(line).get("mdc");

        assertThat(mdc.get("TraceId").asText()).isEqualTo("0af7651916cd43dd8448eb211c80319c");
        assertThat(mdc.get("SpanId").asText()).isEqualTo("b7ad6b7169203331");
        assertThat(mdc.has("traceId")).isFalse();
        assertThat(mdc.has("spanId")).isFalse();
        // Keys without an override are kept as they are
        assertThat(mdc.get("userId").asText()).isEqualTo("42");
    }

    @Test
    public void flatMdcKeysAreRenamedTest() throws Exception {
        JsonFormatter formatter = new JsonFormatter();
        formatter.setFlatMdc(true);
        formatter.setMdcKeyOverrides(Map.of("traceId", "TraceId"));

        String line = formatter.format(recordWithTraceMdc());
        JsonNode node = new ObjectMapper().readTree(line);

        assertThat(node.has("mdc")).isFalse();
        assertThat(node.get("TraceId").asText()).isEqualTo("0af7651916cd43dd8448eb211c80319c");
        assertThat(node.has("traceId")).isFalse();
        assertThat(node.get("spanId").asText()).isEqualTo("b7ad6b7169203331");
        assertThat(node.get("userId").asText()).isEqualTo("42");
    }

    @Test
    public void mdcKeyOverridesAreAppliedToTheRenamedMdcFieldTest() throws Exception {
        // The mdc field itself is renamed with key-overrides, its entries with mdc.key-overrides
        JsonFormatter formatter = new JsonFormatter("mdc=fields");
        formatter.setMdcKeyOverrides(Map.of("traceId", "TraceId"));

        String line = formatter.format(recordWithTraceMdc());
        JsonNode node = new ObjectMapper().readTree(line);

        assertThat(node.has("mdc")).isFalse();
        assertThat(node.get("fields").get("TraceId").asText()).isEqualTo("0af7651916cd43dd8448eb211c80319c");
    }

    @Test
    public void defaultBehaviorKeepsMdcKeysTest() throws Exception {
        // Regression: a formatter without overrides must write the MDC keys unchanged.
        JsonFormatter formatter = new JsonFormatter();
        assertThat(formatter.getMdcKeyOverrides()).isEmpty();

        String line = formatter.format(recordWithTraceMdc());
        JsonNode mdc = new ObjectMapper().readTree(line).get("mdc");

        assertThat(mdc.get("traceId").asText()).isEqualTo("0af7651916cd43dd8448eb211c80319c");
        assertThat(mdc.has("TraceId")).isFalse();
    }

    private static ExtLogRecord recordWithTraceMdc() {
        ExtLogRecord record = new ExtLogRecord(Level.INFO, "MDC key overrides test",
                ConsoleJsonFormatterMdcKeyOverridesTest.class.getName());
        record.putMdc("traceId", "0af7651916cd43dd8448eb211c80319c");
        record.putMdc("spanId", "b7ad6b7169203331");
        record.putMdc("userId", "42");
        return record;
    }
}
