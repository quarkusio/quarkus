package io.quarkus.it.logging.json;

import io.quarkus.test.QuarkusUnitTest;
import org.jboss.logmanager.handlers.AsyncHandler;
import org.jboss.logmanager.handlers.SocketHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.Level;

import static io.quarkus.it.logging.json.LoggingTestsHelper.getHandler;;
import static org.assertj.core.api.Assertions.assertThat;

public class AsyncSocketHandlerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-async-socket-output.properties")
            .withApplicationRoot((jar) -> jar
                    .addClass(LoggingTestsHelper.class)
                    .addAsManifestResource("application.properties", "microprofile-config.properties"))
            .setLogFileName("AsyncSyslogHandlerTest.log");

    @Test
    public void asyncSyslogHandlerConfigurationTest() throws NullPointerException {
        Handler handler = getHandler(AsyncHandler.class);
        assertThat(handler.getLevel()).isEqualTo(Level.WARNING);

        AsyncHandler asyncHandler = (AsyncHandler) handler;
        assertThat(asyncHandler.getHandlers()).isNotEmpty();
        assertThat(asyncHandler.getQueueLength()).isEqualTo(256);
        assertThat(asyncHandler.getOverflowAction()).isEqualTo(AsyncHandler.OverflowAction.DISCARD);

        Handler nestedSyslogHandler = Arrays.stream(asyncHandler.getHandlers())
                .filter(h -> (h instanceof SocketHandler))
                .findFirst().get();

        SocketHandler socketHandler = (SocketHandler) nestedSyslogHandler;
        assertThat(socketHandler.getPort()).isEqualTo(5140);
        assertThat(socketHandler.getAddress().getHostAddress()).isEqualTo("127.0.0.1");
        assertThat(socketHandler.getProtocol()).isEqualTo(SocketHandler.Protocol.TCP);
        assertThat(socketHandler.isBlockOnReconnect()).isEqualTo(false);
    }
}
