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
import org.jboss.logmanager.handlers.ConsoleHandler;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.bootstrap.logging.QuarkusDelayedHandler;
import io.quarkus.logging.json.runtime.JsonFormatter;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.core.runtime.VertxMDC;

public class ConsoleJsonFormatterGCPConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("""
                            quarkus.log.level=INFO
                            quarkus.log.console.enabled=true
                            quarkus.log.console.level=WARNING
                            quarkus.log.console.format=%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n
                            quarkus.log.console.json.enabled=true
                            quarkus.log.console.json.log-format=gcp
                            """), "application.properties"));

    @Test
    public void jsonFormatterDefaultConfigurationTest() {
        VertxMDC instance = VertxMDC.INSTANCE;
        instance.put("traceId", "aaaaaaaaaaaaaaaaaaaaaaaa");
        instance.put("spanId", "bbbbbbbbbbbbbb");
        instance.put("sampled", "true");

        JsonFormatter jsonFormatter = getJsonFormatter();
        assertThat(jsonFormatter.isPrettyPrint()).isFalse();
        assertThat(jsonFormatter.getDateTimeFormatter().toString())
                .isEqualTo(DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault()).toString());
        assertThat(jsonFormatter.getDateTimeFormatter().getZone()).isEqualTo(ZoneId.systemDefault());
        assertThat(jsonFormatter.getExceptionOutputType()).isEqualTo(StructuredFormatter.ExceptionOutputType.DETAILED);
        assertThat(jsonFormatter.getRecordDelimiter()).isEqualTo("\n");
        assertThat(jsonFormatter.isPrintDetails()).isFalse();
        assertThat(jsonFormatter.getExcludedKeys()).isEmpty();
        assertThat(jsonFormatter.getAdditionalFields().entrySet()).isNotEmpty();
        assertThat(jsonFormatter.getAdditionalFields().get("trace")).isNotNull();
        assertThat(jsonFormatter.getAdditionalFields().get("spanId")).isNotNull();
        assertThat(jsonFormatter.getAdditionalFields().get("traceSampled")).isNotNull();

        instance.remove("traceId");
        instance.remove("spanId");
        instance.remove("sampled");
    }

    public static JsonFormatter getJsonFormatter() {
        LogManager logManager = LogManager.getLogManager();
        assertThat(logManager).isInstanceOf(org.jboss.logmanager.LogManager.class);

        QuarkusDelayedHandler delayedHandler = InitialConfigurator.DELAYED_HANDLER;
        assertThat(Logger.getLogger("").getHandlers()).contains(delayedHandler);
        assertThat(delayedHandler.getLevel()).isEqualTo(Level.ALL);

        Handler handler = Arrays.stream(delayedHandler.getHandlers())
                .filter(h -> (h instanceof ConsoleHandler))
                .findFirst().orElse(null);
        assertThat(handler).isNotNull();
        assertThat(handler.getLevel()).isEqualTo(Level.WARNING);

        Formatter formatter = handler.getFormatter();
        assertThat(formatter).isInstanceOf(JsonFormatter.class);
        return (JsonFormatter) formatter;
    }
}
