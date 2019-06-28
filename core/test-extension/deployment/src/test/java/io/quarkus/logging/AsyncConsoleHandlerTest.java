package io.quarkus.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.jboss.logmanager.handlers.AsyncHandler;
import org.jboss.logmanager.handlers.ConsoleHandler;
import org.jboss.logmanager.handlers.DelayedHandler;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.logging.InitialConfigurator;
import io.quarkus.test.QuarkusUnitTest;

public class AsyncConsoleHandlerTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("application-async-console-log.properties", "application.properties"))
            .setLogFileName("AsyncConsoleHandlerTest.log");

    @Test
    public void asyncConsoleHandlerConfigurationTest() {
        LogManager logManager = LogManager.getLogManager();
        assertThat(logManager).isInstanceOf(org.jboss.logmanager.LogManager.class);

        DelayedHandler delayedHandler = InitialConfigurator.DELAYED_HANDLER;
        assertThat(Logger.getLogger("").getHandlers()).contains(delayedHandler);
        assertThat(delayedHandler.getLevel()).isEqualTo(Level.ALL);

        Handler handler = Arrays.asList(delayedHandler.getHandlers()).stream()
                .filter(h -> (h instanceof AsyncHandler))
                .findFirst().get();
        assertThat(handler).isNotNull();
        assertThat(handler.getLevel()).isEqualTo(Level.WARNING);

        AsyncHandler asyncHandler = (AsyncHandler) handler;
        assertThat(asyncHandler.getHandlers()).isNotEmpty();
        assertThat(asyncHandler.getQueueLength()).isEqualTo(256);
        assertThat(asyncHandler.getOverflowAction()).isEqualTo(AsyncHandler.OverflowAction.DISCARD);

        Handler nestedConsoleHandler = Arrays.asList(asyncHandler.getHandlers()).stream()
                .filter(h -> (h instanceof ConsoleHandler))
                .findFirst().get();

        ConsoleHandler consoleHandler = (ConsoleHandler) nestedConsoleHandler;
        assertThat(consoleHandler.getLevel()).isEqualTo(Level.WARNING);

    }

}
