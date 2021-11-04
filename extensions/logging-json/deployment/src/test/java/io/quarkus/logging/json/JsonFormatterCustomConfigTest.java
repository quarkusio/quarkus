package io.quarkus.logging.json;

import static io.quarkus.logging.json.JsonFormatterDefaultConfigTest.getJsonFormatter;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;

import org.jboss.logmanager.formatters.JsonFormatter;
import org.jboss.logmanager.formatters.StructuredFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class JsonFormatterCustomConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(JsonFormatterDefaultConfigTest.class))
            .withConfigurationResource("application-json-formatter-custom.properties");

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
    }
}
