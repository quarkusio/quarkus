package io.quarkus.logging.json;

import static io.quarkus.logging.json.ConsoleJsonFormatterDefaultConfigTest.getJsonFormatter;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.json.runtime.JsonFormatter.JsonLogGenerator;
import io.quarkus.logging.json.runtime.JsonProvider;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.core.runtime.VertxMDC;

public class ConsoleJsonFormatterJsonProviderTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(ConsoleJsonFormatterDefaultConfigTest.class,
                    CustomJsonProvider.class, AuditFieldProvider.class,
                    LevelBasedProvider.class, MdcFieldProvider.class, NumericFieldProvider.class,
                    MetadataProvider.class))
            .withConfigurationResource("application-console-json-formatter-default.properties");

    @Test
    public void jsonProviderFieldsAreAddedToOutput() throws Exception {
        String line = getJsonFormatter().format(new LogRecord(Level.INFO, "Hello from provider test"));

        JsonNode node = new ObjectMapper().readTree(line);
        assertThat(node.has("privatemessage")).isTrue();
        assertThat(node.get("privatemessage").asText()).isEqualTo("Hello from provider test");
        assertThat(node.has("datamaskingfields")).isTrue();
        assertThat(node.get("datamaskingfields").asText()).isEqualTo("none");
    }

    @Test
    public void jsonProviderConditionalFieldsBasedOnLoggerPackage() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        LogRecord auditRecord = new LogRecord(Level.INFO, "Sensitive operation");
        auditRecord.setLoggerName("audit.security");
        JsonNode auditNode = mapper.readTree(getJsonFormatter().format(auditRecord));
        assertThat(auditNode.has("auditcategory")).isTrue();
        assertThat(auditNode.get("auditcategory").asText()).isEqualTo("security");

        LogRecord regularRecord = new LogRecord(Level.INFO, "Regular log message");
        regularRecord.setLoggerName("com.example.service");
        JsonNode regularNode = mapper.readTree(getJsonFormatter().format(regularRecord));
        assertThat(regularNode.has("auditcategory")).isFalse();
    }

    @ApplicationScoped
    public static class CustomJsonProvider implements JsonProvider {

        @Override
        public void writeTo(JsonLogGenerator generator, ExtLogRecord record) throws Exception {
            generator.add("privatemessage", record.getFormattedMessage());
            generator.add("datamaskingfields", "none");
        }
    }

    @Test
    public void jsonProviderLevelBasedConditional() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        LogRecord errorRecord = new LogRecord(org.jboss.logmanager.Level.ERROR, "Something went wrong");
        JsonNode errorNode = mapper.readTree(getJsonFormatter().format(errorRecord));
        assertThat(errorNode.has("alert")).isTrue();
        assertThat(errorNode.get("alert").asText()).isEqualTo("true");

        LogRecord infoRecord = new LogRecord(Level.INFO, "Everything is fine");
        JsonNode infoNode = mapper.readTree(getJsonFormatter().format(infoRecord));
        assertThat(infoNode.has("alert")).isFalse();
    }

    @Test
    public void jsonProviderMdcFieldInjection() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        VertxMDC.INSTANCE.put("requestId", "req-12345");
        try {
            JsonNode node = mapper.readTree(getJsonFormatter().format(new LogRecord(Level.INFO, "Request processed")));
            assertThat(node.has("requestId")).isTrue();
            assertThat(node.get("requestId").asText()).isEqualTo("req-12345");
        } finally {
            VertxMDC.INSTANCE.remove("requestId");
        }

        JsonNode node = mapper.readTree(getJsonFormatter().format(new LogRecord(Level.INFO, "No MDC")));
        assertThat(node.has("requestId")).isFalse();
    }

    @Test
    public void jsonProviderNumericTypes() throws Exception {
        JsonNode node = new ObjectMapper().readTree(
                getJsonFormatter().format(new LogRecord(Level.INFO, "Numeric fields test")));

        assertThat(node.has("responseCode")).isTrue();
        assertThat(node.get("responseCode").isInt()).isTrue();
        assertThat(node.get("responseCode").asInt()).isEqualTo(200);

        assertThat(node.has("responseTimeMs")).isTrue();
        assertThat(node.get("responseTimeMs").isLong()).isTrue();
        assertThat(node.get("responseTimeMs").asLong()).isEqualTo(5_000_000_000L);
    }

    @ApplicationScoped
    public static class AuditFieldProvider implements JsonProvider {

        @Override
        public void writeTo(JsonLogGenerator generator, ExtLogRecord record) throws Exception {
            String loggerName = record.getLoggerName();
            if (loggerName != null && loggerName.startsWith("audit.")) {
                generator.add("auditcategory", loggerName.substring("audit.".length()));
            }
        }
    }

    @ApplicationScoped
    public static class LevelBasedProvider implements JsonProvider {

        @Override
        public void writeTo(JsonLogGenerator generator, ExtLogRecord record) throws Exception {
            if (record.getLevel().intValue() >= org.jboss.logmanager.Level.ERROR.intValue()) {
                generator.add("alert", true);
            }
        }
    }

    @ApplicationScoped
    public static class MdcFieldProvider implements JsonProvider {

        @Override
        public void writeTo(JsonLogGenerator generator, ExtLogRecord record) throws Exception {
            String requestId = record.getMdcCopy().get("requestId");
            if (requestId != null) {
                generator.add("requestId", requestId);
            }
        }
    }

    @ApplicationScoped
    public static class NumericFieldProvider implements JsonProvider {

        @Override
        public void writeTo(JsonLogGenerator generator, ExtLogRecord record) throws Exception {
            generator.add("responseCode", 200);
            generator.add("responseTimeMs", 5_000_000_000L);
        }
    }

    @Test
    public void jsonProviderCanWriteNestedObject() throws Exception {
        String line = getJsonFormatter().format(new LogRecord(Level.INFO, "Nested object test"));

        JsonNode node = new ObjectMapper().readTree(line);
        assertThat(node.has("metadata")).isTrue();
        assertThat(node.get("metadata").isObject()).isTrue();
        assertThat(node.get("metadata").get("version").asText()).isEqualTo("1.0");
        assertThat(node.get("metadata").get("source").asText()).isEqualTo("test");
    }

    @ApplicationScoped
    public static class MetadataProvider implements JsonProvider {

        @Override
        public void writeTo(JsonLogGenerator generator, ExtLogRecord record) throws Exception {
            generator.startObject("metadata")
                    .add("version", "1.0")
                    .add("source", "test")
                    .endObject();
        }
    }
}
