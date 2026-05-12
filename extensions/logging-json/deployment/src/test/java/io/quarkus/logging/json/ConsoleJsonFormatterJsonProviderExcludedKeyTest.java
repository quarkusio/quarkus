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
                            NestedObjectProvider.class)
                    .addAsResource(new StringAsset("""
                            quarkus.log.level=INFO
                            quarkus.log.console.enabled=true
                            quarkus.log.console.level=WARNING
                            quarkus.log.console.json.enabled=true
                            quarkus.log.console.json.excluded-keys=sequence,metadata
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
}
