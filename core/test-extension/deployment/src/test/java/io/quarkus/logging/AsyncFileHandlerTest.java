package io.quarkus.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.jboss.logmanager.handlers.AsyncHandler;
import org.jboss.logmanager.handlers.DelayedHandler;
import org.jboss.logmanager.handlers.FileHandler;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.logging.InitialConfigurator;
import io.quarkus.test.QuarkusUnitTest;

public class AsyncFileHandlerTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("application-async-file-log.properties", "application.properties"))
            .setLogFileName("AsyncFileHandlerTest.log");

    @Test
    public void asyncFileHandlerConfigurationTest() {
        LogManager logManager = LogManager.getLogManager();
        assertThat(logManager).isInstanceOf(org.jboss.logmanager.LogManager.class);

        DelayedHandler delayedHandler = InitialConfigurator.DELAYED_HANDLER;
        assertThat(Logger.getLogger("").getHandlers()).contains(delayedHandler);
        assertThat(delayedHandler.getLevel()).isEqualTo(Level.ALL);

        Handler handler = Arrays.asList(delayedHandler.getHandlers()).stream()
                .filter(h -> (h instanceof AsyncHandler))
                .findFirst().get();
        assertThat(handler).isNotNull();
        assertThat(handler.getLevel()).isEqualTo(Level.INFO);

        AsyncHandler asyncHandler = (AsyncHandler) handler;
        assertThat(asyncHandler.getHandlers()).isNotEmpty();
        assertThat(asyncHandler.getQueueLength()).isEqualTo(1024);
        assertThat(asyncHandler.getOverflowAction()).isEqualTo(AsyncHandler.OverflowAction.BLOCK);

        Handler nestedFileHandler = Arrays.asList(asyncHandler.getHandlers()).stream()
                .filter(h -> (h instanceof FileHandler))
                .findFirst().get();

        FileHandler fileHandler = (FileHandler) nestedFileHandler;
        assertThat(fileHandler.getLevel()).isEqualTo(Level.INFO);

    }
}
