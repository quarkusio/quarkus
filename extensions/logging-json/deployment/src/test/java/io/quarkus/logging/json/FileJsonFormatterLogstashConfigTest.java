package io.quarkus.logging.json;

import static io.quarkus.logging.json.FileJsonFormatterDefaultConfigTest.getJsonFormatter;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.LogRecord;

import org.jboss.logmanager.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.logging.json.runtime.JsonFormatter;
import io.quarkus.test.QuarkusExtensionTest;
import tools.jackson.databind.JsonNode;

public class FileJsonFormatterLogstashConfigTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(FileJsonFormatterDefaultConfigTest.class, JacksonUtil.class))
            .overrideConfigKey("quarkus.log.level", "INFO")
            .overrideConfigKey("quarkus.log.file.enabled", "true")
            .overrideConfigKey("quarkus.log.file.level", "WARNING")
            .overrideConfigKey("quarkus.log.file.json.enabled", "true")
            .overrideConfigKey("quarkus.log.file.json.log-format", "logstash");

    @Test
    public void jsonFormatterLogstashOutputTest() throws Exception {
        JsonFormatter jsonFormatter = getJsonFormatter();

        assertThat(jsonFormatter.isFlatMdc()).isTrue();
        assertThat(jsonFormatter.getAdditionalFields().get("@version").value()).isEqualTo("1");

        JsonNode node = JacksonUtil.MAPPER.readTree(jsonFormatter.format(new LogRecord(Level.INFO, "Hello Logstash")));
        assertThat(node.get("@version").asText()).isEqualTo("1");
        assertThat(node.get("@timestamp")).isNotNull();
        assertThat(node.get("logger_name")).isNotNull();
        assertThat(node.get("level_value").asInt()).isEqualTo(20_000);
        assertThat(node.has("sequence")).isFalse();
    }
}
