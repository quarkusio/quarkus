package io.quarkus.it.logging.json;

import io.quarkus.test.QuarkusUnitTest;
import org.jboss.logmanager.formatters.JsonFormatter;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.SizeRotatingFileHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;

import static io.quarkus.it.logging.json.LoggingTestsHelper.getHandler;;
import static org.assertj.core.api.Assertions.assertThat;

public class SizeRotatingLoggingTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-size-file-log-rotating.properties")
            .withApplicationRoot((jar) -> jar
                    .addClass(LoggingTestsHelper.class)
                    .addAsManifestResource("application.properties", "microprofile-config.properties"))
            .setLogFileName("SizeRotatingLoggingTest.log");

    @Test
    public void sizeRotatingConfigurationTest() {
        Handler handler = getHandler(SizeRotatingFileHandler.class);
        assertThat(handler.getLevel()).isEqualTo(Level.INFO);

        Formatter formatter = handler.getFormatter();
        assertThat(formatter).isInstanceOf(JsonFormatter.class);
        SizeRotatingFileHandler sizeRotatingFileHandler = (SizeRotatingFileHandler) handler;
        assertThat(sizeRotatingFileHandler.isRotateOnBoot()).isTrue();
    }
}
