package io.quarkus.it.logging.json;

import io.quarkus.test.QuarkusUnitTest;
import org.jboss.logmanager.formatters.JsonFormatter;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.PeriodicSizeRotatingFileHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;

import static io.quarkus.it.logging.json.LoggingTestsHelper.getHandler;;
import static org.assertj.core.api.Assertions.assertThat;

public class PeriodicSizeRotatingLoggingRotateOnBootTest {

    private static final String FILE_NAME = PeriodicSizeRotatingLoggingRotateOnBootTest.class.getSimpleName() + ".log";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-periodic-size-file-log-rotating-rotate-on-boot.properties")
            .withApplicationRoot((jar) -> jar
                    .addClass(LoggingTestsHelper.class)
                    .addAsManifestResource("application.properties", "microprofile-config.properties"))
            .setLogFileName(FILE_NAME);

    @Test
    public void periodicSizeRotatingConfigurationTest() {
        Handler handler = getHandler(PeriodicSizeRotatingFileHandler.class);
        assertThat(handler.getLevel()).isEqualTo(Level.INFO);

        Formatter formatter = handler.getFormatter();
        assertThat(formatter).isInstanceOf(JsonFormatter.class);

        PeriodicSizeRotatingFileHandler periodicSizeRotatingFileHandler = (PeriodicSizeRotatingFileHandler) handler;
        assertThat(periodicSizeRotatingFileHandler.isRotateOnBoot()).isTrue();
    }
}
