package io.quarkus.logging.json;

import static io.quarkus.logging.json.SocketJsonFormatterDefaultConfigTest.getJsonFormatter;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.assertj.core.api.Assertions;
import org.jboss.logmanager.formatters.StructuredFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.json.runtime.JsonFormatter;
import io.quarkus.logging.json.runtime.JsonLogConfig.AdditionalFieldConfig;
import io.quarkus.test.QuarkusUnitTest;

public class SocketJsonFormatterCustomConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(SocketJsonFormatterDefaultConfigTest.class))
            .withConfigurationResource("application-socket-json-formatter-custom.properties");

    @Test
    public void jsonFormatterCustomConfigurationTest() {
        JsonFormatter jsonFormatter = getJsonFormatter();
        assertThat(jsonFormatter.isPrettyPrint()).isTrue();
        assertThat(jsonFormatter.getDateTimeFormatter().toString())
                .isEqualTo("Value(DayOfMonth)' 'Text(MonthOfYear,SHORT)' 'Value(Year,4,19,EXCEEDS_PAD)");
        assertThat(jsonFormatter.getDateTimeFormatter().getZone()).isEqualTo(ZoneId.of("UTC+05:00"));
        assertThat(jsonFormatter.getExceptionOutputType())
                .isEqualTo(StructuredFormatter.ExceptionOutputType.DETAILED_AND_FORMATTED);
        assertThat(jsonFormatter.getRecordDelimiter()).isEqualTo("\n;");
        assertThat(jsonFormatter.isPrintDetails()).isTrue();
        assertThat(jsonFormatter.getExcludedKeys()).containsExactlyInAnyOrder("timestamp", "sequence");
        assertThat(jsonFormatter.getAdditionalFields().size()).isEqualTo(2);
        assertThat(jsonFormatter.getAdditionalFields().containsKey("foo")).isTrue();
        assertThat(jsonFormatter.getAdditionalFields().get("foo").type()).isEqualTo(AdditionalFieldConfig.Type.INT);
        assertThat(jsonFormatter.getAdditionalFields().get("foo").value()).isEqualTo("42");
        assertThat(jsonFormatter.getAdditionalFields().containsKey("bar")).isTrue();
        assertThat(jsonFormatter.getAdditionalFields().get("bar").type()).isEqualTo(AdditionalFieldConfig.Type.STRING);
        assertThat(jsonFormatter.getAdditionalFields().get("bar").value()).isEqualTo("baz");
    }

    @Test
    public void jsonFormatterOutputTest() throws Exception {
        JsonFormatter jsonFormatter = getJsonFormatter();
        String line = jsonFormatter.format(new LogRecord(Level.INFO, "Hello, World!"));

        JsonNode node = new ObjectMapper().readTree(line);
        // "level" has been renamed to HEY
        Assertions.assertThat(node.has("level")).isFalse();
        Assertions.assertThat(node.has("HEY")).isTrue();
        Assertions.assertThat(node.get("HEY").asText()).isEqualTo("INFO");

        // excluded fields
        Assertions.assertThat(node.has("timestamp")).isFalse();
        Assertions.assertThat(node.has("sequence")).isFalse();

        // additional fields
        Assertions.assertThat(node.has("foo")).isTrue();
        Assertions.assertThat(node.get("foo").asInt()).isEqualTo(42);
        Assertions.assertThat(node.has("bar")).isTrue();
        Assertions.assertThat(node.get("bar").asText()).isEqualTo("baz");
        Assertions.assertThat(node.get("message").asText()).isEqualTo("Hello, World!");
    }
}
