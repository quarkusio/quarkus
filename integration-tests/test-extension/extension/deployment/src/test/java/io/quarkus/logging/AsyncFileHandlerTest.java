package io.quarkus.logging;

import static io.quarkus.logging.LoggingTestsHelper.getHandler;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.jboss.logmanager.handlers.AsyncHandler;
import org.jboss.logmanager.handlers.FileHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class AsyncFileHandlerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-async-file-log.properties")
            .withApplicationRoot((jar) -> jar
                    .addClass(LoggingTestsHelper.class)
                    .addAsManifestResource("application.properties", "microprofile-config.properties"))
            .setLogFileName("AsyncFileHandlerTest.log");

    @Test
    public void asyncFileHandlerConfigurationTest() {
        Handler handler = getHandler(AsyncHandler.class);
        assertThat(handler.getLevel()).isEqualTo(Level.INFO);

        AsyncHandler asyncHandler = (AsyncHandler) handler;
        assertThat(asyncHandler.getHandlers()).isNotEmpty();
        assertThat(asyncHandler.getQueueLength()).isEqualTo(1024);
        assertThat(asyncHandler.getOverflowAction()).isEqualTo(AsyncHandler.OverflowAction.BLOCK);

        Handler nestedFileHandler = Arrays.stream(asyncHandler.getHandlers())
                .filter(h -> (h instanceof FileHandler))
                .findFirst().get();

        FileHandler fileHandler = (FileHandler) nestedFileHandler;
        assertThat(fileHandler.getLevel()).isEqualTo(Level.INFO);

    }
}
