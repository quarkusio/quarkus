package io.quarkus.logging.json;

import static io.quarkus.logging.json.ConsoleJsonFormatterDefaultConfigTest.getJsonFormatter;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.json.runtime.JsonFormatter.JsonLogGenerator;
import io.quarkus.logging.json.runtime.JsonProvider;
import io.quarkus.test.QuarkusExtensionTest;

public class ConsoleJsonFormatterServiceLoaderProviderTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ConsoleJsonFormatterDefaultConfigTest.class, ServiceLoaderJsonProvider.class)
                    .addAsServiceProvider(JsonProvider.class, ServiceLoaderJsonProvider.class))
            .withConfigurationResource("application-console-json-formatter-default.properties");

    @Test
    public void serviceLoaderProviderFieldsAreAddedToOutput() throws Exception {
        String line = getJsonFormatter().format(new LogRecord(Level.INFO, "Hello from service loader test"));

        JsonNode node = new ObjectMapper().readTree(line);
        assertThat(node.has("source")).isTrue();
        assertThat(node.get("source").asText()).isEqualTo("serviceloader");
    }

    /**
     * A plain (non-CDI) JsonProvider registered via the ServiceLoader mechanism.
     */
    public static class ServiceLoaderJsonProvider implements JsonProvider {

        @Override
        public void writeTo(JsonLogGenerator generator, ExtLogRecord record) throws Exception {
            generator.add("source", "serviceloader");
        }
    }
}
