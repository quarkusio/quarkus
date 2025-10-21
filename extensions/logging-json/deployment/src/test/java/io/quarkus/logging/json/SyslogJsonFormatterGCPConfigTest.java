package io.quarkus.logging.json;

import static io.quarkus.logging.json.SyslogJsonFormatterDefaultConfigTest.getJsonFormatter;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.assertj.core.api.Assertions;
import org.jboss.logmanager.formatters.StructuredFormatter;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.json.runtime.JsonFormatter;
import io.quarkus.logging.json.runtime.JsonLogConfig.AdditionalFieldConfig;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.core.runtime.VertxMDC;

public class SyslogJsonFormatterGCPConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SyslogJsonFormatterDefaultConfigTest.class)
                    .addAsResource(new StringAsset("""
                            quarkus.log.level=INFO
                            quarkus.log.syslog.enabled=true
                            quarkus.log.syslog.level=WARNING
                            quarkus.log.syslog.json.enabled=true
                            quarkus.log.syslog.json.pretty-print=true
                            quarkus.log.syslog.json.date-format=d MMM uuuu
                            quarkus.log.syslog.json.record-delimiter=\\n;
                            quarkus.log.syslog.json.zone-id=UTC+05:00
                            quarkus.log.syslog.json.exception-output-type=DETAILED_AND_FORMATTED
                            quarkus.log.syslog.json.print-details=true
                            quarkus.log.syslog.json.key-overrides=level=HEY
                            quarkus.log.syslog.json.excluded-keys=timestamp,sequence
                            quarkus.log.syslog.json.additional-field.foo.value=42
                            quarkus.log.syslog.json.additional-field.foo.type=int
                            quarkus.log.syslog.json.additional-field.bar.value=baz
                            quarkus.log.syslog.json.additional-field.bar.type=string
                            quarkus.log.syslog.json.log-format=gcp
                            """), "application.properties"));

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
        assertThat(jsonFormatter.getAdditionalFields().size()).isEqualTo(5);
        assertThat(jsonFormatter.getAdditionalFields().containsKey("foo")).isTrue();
        assertThat(jsonFormatter.getAdditionalFields().get("foo").type()).isEqualTo(AdditionalFieldConfig.Type.INT);
        assertThat(jsonFormatter.getAdditionalFields().get("foo").value()).isEqualTo("42");
        assertThat(jsonFormatter.getAdditionalFields().containsKey("bar")).isTrue();
        assertThat(jsonFormatter.getAdditionalFields().get("bar").type()).isEqualTo(AdditionalFieldConfig.Type.STRING);
        assertThat(jsonFormatter.getAdditionalFields().get("bar").value()).isEqualTo("baz");
        assertThat(jsonFormatter.getAdditionalFields().containsKey("trace")).isTrue();
        assertThat(jsonFormatter.getAdditionalFields().get("trace").type()).isEqualTo(AdditionalFieldConfig.Type.STRING);
        assertThat(jsonFormatter.getAdditionalFields().containsKey("spanId")).isTrue();
        assertThat(jsonFormatter.getAdditionalFields().get("spanId").type()).isEqualTo(AdditionalFieldConfig.Type.STRING);
        assertThat(jsonFormatter.getAdditionalFields().containsKey("traceSampled")).isTrue();
        assertThat(jsonFormatter.getAdditionalFields().get("traceSampled").type()).isEqualTo(AdditionalFieldConfig.Type.STRING);
    }

    @Test
    public void jsonFormatterOutputTest() throws Exception {
        VertxMDC instance = VertxMDC.INSTANCE;
        instance.put("traceId", "aaaaaaaaaaaaaaaaaaaaaaaab");
        instance.put("spanId", "bbbbbbbbbbbbbbc");
        instance.put("sampled", "false");

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
        Assertions.assertThat(node.has("trace")).isTrue();
        Assertions.assertThat(node.get("trace").asText())
                .isEqualTo("projects/quarkus-logging-json-deployment/traces/aaaaaaaaaaaaaaaaaaaaaaaab");
        Assertions.assertThat(node.has("spanId")).isTrue();
        Assertions.assertThat(node.get("spanId").asText()).isEqualTo("bbbbbbbbbbbbbbc");
        Assertions.assertThat(node.has("traceSampled")).isTrue();
        Assertions.assertThat(node.get("traceSampled").asText()).isEqualTo("false");

        instance.remove("traceId");
        instance.remove("spanId");
        instance.remove("sampled");
    }
}
