package io.quarkus.logging.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Handler;

import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.FileHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.logging.json.runtime.JsonFormatter;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Reproducer for https://github.com/quarkusio/quarkus/issues/47169
 */
public class NamedFileJsonFormatterCustomFormatTest extends AbstractNamedJsonFormatterTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("application-named-file-json-formatter-custom-format.properties");

    @Test
    public void namedHandlerWithoutFormatInheritsGlobalJsonFormatter() {
        Handler handler = getNamedHandler("named.file.inherit", FileHandler.class);
        assertThat(handler).isNotNull();
        assertThat(handler.getFormatter()).isInstanceOf(JsonFormatter.class);
    }

    @Test
    public void namedHandlerWithCustomFormatUsesItsFormat() {
        Handler handler = getNamedHandler("named.file.custom", FileHandler.class);
        assertThat(handler).isNotNull();
        assertThat(handler.getFormatter()).isInstanceOfSatisfying(PatternFormatter.class,
                formatter -> assertThat(formatter.getPattern()).isEqualTo("%d{yyyy-MM-dd HH:mm:ss} [%p] %s%e%n"));
    }

    @Test
    public void namedHandlerWithCustomFormatAndJsonEnabledUsesJsonFormatter() {
        Handler handler = getNamedHandler("named.file.custom.json", FileHandler.class);
        assertThat(handler).isNotNull();
        assertThat(handler.getFormatter()).isInstanceOf(JsonFormatter.class);
    }
}
