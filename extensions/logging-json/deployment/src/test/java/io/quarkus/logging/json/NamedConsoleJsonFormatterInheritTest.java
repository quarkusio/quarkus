package io.quarkus.logging.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Handler;

import org.jboss.logmanager.handlers.ConsoleHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.logging.json.runtime.JsonFormatter;
import io.quarkus.test.QuarkusExtensionTest;

public class NamedConsoleJsonFormatterInheritTest extends AbstractNamedJsonFormatterTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("application-named-console-json-formatter-inherit.properties");

    @Test
    public void namedHandlerInheritsGlobalJsonFormatter() {
        Handler handler = getNamedHandler("named.console.inherit", ConsoleHandler.class);
        assertThat(handler).isNotNull();
        assertThat(handler.getFormatter()).isInstanceOf(JsonFormatter.class);
    }

    @Test
    public void namedHandlerWithJsonDisabledOverridesGlobalFormatter() {
        Handler handler = getNamedHandler("named.console.override", ConsoleHandler.class);
        assertThat(handler).isNotNull();
        assertThat(handler.getFormatter()).isNotInstanceOf(JsonFormatter.class);
    }
}
