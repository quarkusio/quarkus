package io.quarkus.logging;

import static io.quarkus.logging.LoggingTestsHelper.getHandler;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.jboss.logmanager.handlers.AsyncHandler;
import org.jboss.logmanager.handlers.SyslogHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class AsyncSyslogHandlerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-async-syslog.properties")
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
                .filter(h -> (h instanceof SyslogHandler))
                .findFirst().get();

        SyslogHandler syslogHandler = (SyslogHandler) nestedSyslogHandler;
        assertThat(syslogHandler.getPort()).isEqualTo(5144);
        assertThat(syslogHandler.getAppName()).isEqualTo("quarkus");
        assertThat(syslogHandler.getHostname()).isEqualTo("quarkus-test");
        assertThat(syslogHandler.getFacility()).isEqualTo(SyslogHandler.Facility.LOG_ALERT);
        assertThat(syslogHandler.getSyslogType()).isEqualTo(SyslogHandler.SyslogType.RFC3164);
        assertThat(syslogHandler.getProtocol()).isEqualTo(SyslogHandler.Protocol.UDP);
        assertThat(syslogHandler.isUseCountingFraming()).isEqualTo(true);
        assertThat(syslogHandler.isTruncate()).isEqualTo(false);
        assertThat(syslogHandler.isBlockOnReconnect()).isEqualTo(false);
    }
}
