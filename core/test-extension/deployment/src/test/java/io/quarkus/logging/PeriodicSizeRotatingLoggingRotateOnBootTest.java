package io.quarkus.logging;

import static io.quarkus.logging.LoggingTestsHelper.getHandler;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.PeriodicSizeRotatingFileHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

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
        assertThat(formatter).isInstanceOf(PatternFormatter.class);
        PatternFormatter patternFormatter = (PatternFormatter) formatter;
        assertThat(patternFormatter.getPattern()).isEqualTo("%d{HH:mm:ss} %-5p [%c{2.}]] (%t) %s%e%n");

        PeriodicSizeRotatingFileHandler periodicSizeRotatingFileHandler = (PeriodicSizeRotatingFileHandler) handler;
        assertThat(periodicSizeRotatingFileHandler.isRotateOnBoot()).isTrue();
    }
}
