package io.quarkus.logging.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.jboss.logmanager.formatters.JsonFormatter;
import org.jboss.logmanager.formatters.StructuredFormatter;
import org.jboss.logmanager.handlers.ConsoleHandler;
import org.jboss.logmanager.handlers.DelayedHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.logging.InitialConfigurator;
import io.quarkus.test.QuarkusUnitTest;

public class JsonFormatterCustomConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-json-formatter-custom.properties");

    @Test
    public void jsonFormatterCustomConfigurationTest() {
        LogManager logManager = LogManager.getLogManager();
        assertThat(logManager).isInstanceOf(org.jboss.logmanager.LogManager.class);

        DelayedHandler delayedHandler = InitialConfigurator.DELAYED_HANDLER;
        assertThat(Logger.getLogger("").getHandlers()).contains(delayedHandler);
        assertThat(delayedHandler.getLevel()).isEqualTo(Level.ALL);

        Handler handler = Arrays.stream(delayedHandler.getHandlers())
                .filter(h -> (h instanceof ConsoleHandler))
                .findFirst().orElse(null);
        assertThat(handler).isNotNull();
        assertThat(handler.getLevel()).isEqualTo(Level.WARNING);

        Formatter formatter = handler.getFormatter();
        assertThat(formatter).isInstanceOf(JsonFormatter.class);
        JsonFormatter jsonFormatter = (JsonFormatter) formatter;
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
