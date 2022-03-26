package io.quarkus.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.logging.*;

import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.ConsoleHandler;
import org.jboss.logmanager.handlers.FileHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.bootstrap.logging.QuarkusDelayedHandler;
import io.quarkus.test.QuarkusUnitTest;

public class CategoryConfiguredHandlerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-category-configured-handlers-output.properties")
            .withApplicationRoot((jar) -> jar
                    .addAsManifestResource("application.properties", "microprofile-config.properties"));

    @Test
    public void consoleOutputTest() {
        LogManager logManager = LogManager.getLogManager();
        assertThat(logManager).isInstanceOf(org.jboss.logmanager.LogManager.class);

        QuarkusDelayedHandler delayedHandler = InitialConfigurator.DELAYED_HANDLER;
        assertThat(Logger.getLogger("").getHandlers()).contains(delayedHandler);

        Handler handler = Arrays.stream(delayedHandler.getHandlers()).filter(h -> (h instanceof ConsoleHandler))
                .findFirst().get();
        assertThat(handler).isNotNull();
        assertThat(handler.getLevel()).isEqualTo(Level.WARNING);

        Logger categoryLogger = logManager.getLogger("io.quarkus.category");
        assertThat(categoryLogger).isNotNull();
        assertThat(categoryLogger.getHandlers()).hasSize(2).extracting("class").containsExactlyInAnyOrder(ConsoleHandler.class,
                FileHandler.class);

        Logger otherCategoryLogger = logManager.getLogger("io.quarkus.othercategory");
        assertThat(otherCategoryLogger).isNotNull();
        assertThat(otherCategoryLogger.getHandlers()).hasSize(1).extracting("class")
                .containsExactlyInAnyOrder(ConsoleHandler.class);

        Logger anotherCategoryLogger = logManager.getLogger("io.quarkus.anothercategory");
        assertThat(anotherCategoryLogger).isNotNull();
        assertThat(anotherCategoryLogger.getHandlers()).isEmpty();

        Formatter formatter = handler.getFormatter();
        assertThat(formatter).isInstanceOf(PatternFormatter.class);
        PatternFormatter patternFormatter = (PatternFormatter) formatter;
        assertThat(patternFormatter.getPattern()).isEqualTo("%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n");
    }

}
