package io.quarkus.logging.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Formatter;
import java.util.logging.Handler;

import org.jboss.logmanager.handlers.ConsoleHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.logging.json.runtime.JsonFormatter;
import io.quarkus.test.QuarkusUnitTest;

public class NamedConsoleJsonFormatterTest extends AbstractNamedJsonFormatterTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-named-console-json-formatter.properties");

    @Test
    public void namedJsonHandlerHasJsonFormatter() {
        Handler handler = getNamedHandler("named.console.json", ConsoleHandler.class);
        assertThat(handler).isNotNull();
        assertThat(handler.getFormatter()).isInstanceOf(JsonFormatter.class);
    }

    @Test
    public void namedPlainHandlerHasPatternFormatter() {
        Handler handler = getNamedHandler("named.console.plain", ConsoleHandler.class);
        assertThat(handler).isNotNull();
        Formatter formatter = handler.getFormatter();
        assertThat(formatter).isNotInstanceOf(JsonFormatter.class);
    }

    @Test
    public void namedHandlerWithJsonDisabledHasPatternFormatter() {
        Handler handler = getNamedHandler("named.console.json.disabled", ConsoleHandler.class);
        assertThat(handler).isNotNull();
        assertThat(handler.getFormatter()).isNotInstanceOf(JsonFormatter.class);
    }

}
