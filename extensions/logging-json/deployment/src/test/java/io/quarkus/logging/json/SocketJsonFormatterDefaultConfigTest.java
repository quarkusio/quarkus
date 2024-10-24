package io.quarkus.logging.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.jboss.logmanager.formatters.StructuredFormatter;
import org.jboss.logmanager.handlers.SocketHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.bootstrap.logging.QuarkusDelayedHandler;
import io.quarkus.logging.json.runtime.JsonFormatter;
import io.quarkus.test.QuarkusUnitTest;

public class SocketJsonFormatterDefaultConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-socket-json-formatter-default.properties");

    @Test
    public void jsonFormatterDefaultConfigurationTest() {
        JsonFormatter jsonFormatter = getJsonFormatter();
        assertThat(jsonFormatter.isPrettyPrint()).isFalse();
        assertThat(jsonFormatter.getDateTimeFormatter().toString())
                .isEqualTo(DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault()).toString());
        assertThat(jsonFormatter.getDateTimeFormatter().getZone()).isEqualTo(ZoneId.systemDefault());
        assertThat(jsonFormatter.getExceptionOutputType()).isEqualTo(StructuredFormatter.ExceptionOutputType.DETAILED);
        assertThat(jsonFormatter.getRecordDelimiter()).isEqualTo("\n");
        assertThat(jsonFormatter.isPrintDetails()).isFalse();
        assertThat(jsonFormatter.getExcludedKeys()).isEmpty();
        assertThat(jsonFormatter.getAdditionalFields().entrySet()).isEmpty();
    }

    public static JsonFormatter getJsonFormatter() {
        LogManager logManager = LogManager.getLogManager();
        assertThat(logManager).isInstanceOf(org.jboss.logmanager.LogManager.class);

        QuarkusDelayedHandler delayedHandler = InitialConfigurator.DELAYED_HANDLER;
        assertThat(Logger.getLogger("").getHandlers()).contains(delayedHandler);
        assertThat(delayedHandler.getLevel()).isEqualTo(Level.ALL);

        Handler handler = Arrays.stream(delayedHandler.getHandlers())
                .filter(h -> (h instanceof SocketHandler))
                .findFirst().orElse(null);
        assertThat(handler).isNotNull();
        assertThat(handler.getLevel()).isEqualTo(Level.WARNING);

        Formatter formatter = handler.getFormatter();
        assertThat(formatter).isInstanceOf(JsonFormatter.class);
        return (JsonFormatter) formatter;
    }
}
