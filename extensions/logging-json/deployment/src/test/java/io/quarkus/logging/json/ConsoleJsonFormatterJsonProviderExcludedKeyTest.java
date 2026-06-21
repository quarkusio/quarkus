package io.quarkus.logging.json;

import static io.quarkus.logging.json.ConsoleJsonFormatterDefaultConfigTest.getJsonFormatter;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logmanager.ExtLogRecord;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.json.runtime.JsonFormatter.JsonLogGenerator;
import io.quarkus.logging.json.runtime.JsonProvider;
import io.quarkus.test.QuarkusExtensionTest;

public class ConsoleJsonFormatterJsonProviderExcludedKeyTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ConsoleJsonFormatterDefaultConfigTest.class, SequenceWritingProvider.class,
                            NestedObjectProvider.class, NestedArrayProvider.class, DeeplyNestedObjectProvider.class)
                    .addAsResource(new StringAsset("""
                            quarkus.log.level=INFO
                            quarkus.log.console.enabled=true
                            quarkus.log.console.level=WARNING
                            quarkus.log.console.json.enabled=true
                            quarkus.log.console.json.excluded-keys=sequence,metadata,tags,context
                            """), "application.properties"));

    @Test
    public void excludedKeyIsFilteredEvenWhenWrittenByProvider() throws Exception {
        String line = getJsonFormatter().format(new LogRecord(Level.INFO, "Test message"));

        JsonNode node = new ObjectMapper().readTree(line);
        assertThat(node.has("sequence")).isFalse();
        assertThat(node.has("message")).isTrue();
        assertThat(node.has("customfield")).isTrue();
        assertThat(node.get("customfield").asText()).isEqualTo("present");
    }

    @Test
    public void excludedKeyIsFilteredForNestedObject() throws Exception {
        String line = getJsonFormatter().format(new LogRecord(Level.INFO, "Test message"));

        JsonNode node = new ObjectMapper().readTree(line);
        assertThat(node.has("metadata")).isFalse();
        assertThat(node.has("customfield")).isTrue();
    }

    @ApplicationScoped
    public static class SequenceWritingProvider implements JsonProvider {

        @Override
        public void writeTo(JsonLogGenerator generator, ExtLogRecord record) throws Exception {
            // "sequence" is in excluded-keys — this write should be filtered
            generator.add("sequence", 999);
            // this field is not excluded — should appear
            generator.add("customfield", "present");
        }
    }

    @Test
    public void excludedKeyIsFilteredForArray() throws Exception {
        String line = getJsonFormatter().format(new LogRecord(Level.INFO, "Test message"));

        JsonNode node = new ObjectMapper().readTree(line);
        assertThat(node.has("tags")).isFalse();
        assertThat(node.has("customfield")).isTrue();
    }

    @Test
    public void excludedKeyIsFilteredForDeeplyNestedContent() throws Exception {
        String line = getJsonFormatter().format(new LogRecord(Level.INFO, "Test message"));

        JsonNode node = new ObjectMapper().readTree(line);
        assertThat(node.has("context")).isFalse();
        // nested keys inside the excluded object must not leak to the top level
        assertThat(node.has("inner")).isFalse();
        assertThat(node.has("deepkey")).isFalse();
        assertThat(node.has("customfield")).isTrue();
    }

    @Test
    public void exceptionFormattingWorksWithExcludedKeys() throws Exception {
        LogRecord record = new LogRecord(Level.WARNING, "Something went wrong");
        record.setThrown(new RuntimeException("boom"));

        String line = getJsonFormatter().format(record);

        JsonNode node = new ObjectMapper().readTree(line);
        assertThat(node.has("message")).isTrue();
        assertThat(node.has("exception")).isTrue();
    }

    @ApplicationScoped
    public static class NestedObjectProvider implements JsonProvider {

        @Override
        public void writeTo(JsonLogGenerator generator, ExtLogRecord record) throws Exception {
            // "metadata" is in excluded-keys — the entire nested object should be filtered
            generator.startObject("metadata")
                    .add("version", "1.0")
                    .add("source", "test")
                    .endObject();
        }
    }

    @ApplicationScoped
    public static class NestedArrayProvider implements JsonProvider {

        @Override
        public void writeTo(JsonLogGenerator generator, ExtLogRecord record) throws Exception {
            // "tags" is in excluded-keys — the entire array should be filtered
            generator.startArray("tags")
                    .add("env", "production")
                    .add("region", "us-east-1")
                    .endArray();
            generator.add("customfield", "present");
        }
    }

    @ApplicationScoped
    public static class DeeplyNestedObjectProvider implements JsonProvider {

        @Override
        public void writeTo(JsonLogGenerator generator, ExtLogRecord record) throws Exception {
            // "context" is in excluded-keys — all nested content must be absorbed, not leaked
            generator.startObject("context")
                    .startObject("inner")
                    .add("deepkey", "value")
                    .endObject()
                    .endObject();
            generator.add("customfield", "present");
        }
    }
}
