package io.quarkus.logging;

import static io.quarkus.logging.LoggingTestsHelper.getHandler;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.jboss.logmanager.handlers.AsyncHandler;
import org.jboss.logmanager.handlers.ConsoleHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class AsyncConsoleHandlerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-async-console-log.properties")
            .withApplicationRoot((jar) -> jar
                    .addClass(LoggingTestsHelper.class)
                    .addAsManifestResource("application.properties", "microprofile-config.properties"))
            .setLogFileName("AsyncConsoleHandlerTest.log");

    @Test
    public void asyncConsoleHandlerConfigurationTest() {
        Handler handler = getHandler(AsyncHandler.class);
        assertThat(handler.getLevel()).isEqualTo(Level.WARNING);

        AsyncHandler asyncHandler = (AsyncHandler) handler;
        assertThat(asyncHandler.getHandlers()).isNotEmpty();
        assertThat(asyncHandler.getQueueLength()).isEqualTo(256);
        assertThat(asyncHandler.getOverflowAction()).isEqualTo(AsyncHandler.OverflowAction.DISCARD);

        Handler nestedConsoleHandler = Arrays.stream(asyncHandler.getHandlers())
                .filter(h -> (h instanceof ConsoleHandler))
                .findFirst().get();

        ConsoleHandler consoleHandler = (ConsoleHandler) nestedConsoleHandler;
        assertThat(consoleHandler.getLevel()).isEqualTo(Level.WARNING);

    }

}
