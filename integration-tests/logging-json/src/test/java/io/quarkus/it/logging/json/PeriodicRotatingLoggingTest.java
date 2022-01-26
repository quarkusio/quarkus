package io.quarkus.it.logging.json;

import io.quarkus.test.QuarkusUnitTest;
import org.jboss.logmanager.formatters.JsonFormatter;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.PeriodicRotatingFileHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;

import static io.quarkus.it.logging.json.LoggingTestsHelper.getHandler;;
import static org.assertj.core.api.Assertions.assertThat;

public class PeriodicRotatingLoggingTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-periodic-file-log-rotating.properties")
            .withApplicationRoot((jar) -> jar
                    .addClass(LoggingTestsHelper.class)
                    .addAsManifestResource("application.properties", "microprofile-config.properties"))
            .setLogFileName("PeriodicRotatingLoggingTest.log");

    @Test
    public void periodicRotatingConfigurationTest() {
        Handler handler = getHandler(PeriodicRotatingFileHandler.class);
        assertThat(handler.getLevel()).isEqualTo(Level.INFO);

        Formatter formatter = handler.getFormatter();
        assertThat(formatter).isInstanceOf(JsonFormatter.class);
    }
}
