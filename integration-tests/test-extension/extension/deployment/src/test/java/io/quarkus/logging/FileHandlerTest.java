package io.quarkus.logging;

import static io.quarkus.logging.LoggingTestsHelper.getHandler;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.FileHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FileHandlerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-file-output-log.properties")
            .withApplicationRoot((jar) -> jar
                    .addClass(LoggingTestsHelper.class)
                    .addAsManifestResource("application.properties", "microprofile-config.properties"));

    @Test
    public void fileOutputTest() {
        Handler handler = getHandler(FileHandler.class);
        assertThat(handler.getLevel()).isEqualTo(Level.INFO);

        Formatter formatter = handler.getFormatter();
        assertThat(formatter).isInstanceOf(PatternFormatter.class);

        PatternFormatter patternFormatter = (PatternFormatter) formatter;
        assertThat(patternFormatter.getPattern()).isEqualTo("%d{HH:mm:ss} %-5p [%c{2.}]] (%t) %s%e%n");
    }

}
