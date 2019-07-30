package io.quarkus.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.jboss.logmanager.handlers.AsyncHandler;
import org.jboss.logmanager.handlers.DelayedHandler;
import org.jboss.logmanager.handlers.SyslogHandler;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.logging.InitialConfigurator;
import io.quarkus.test.QuarkusUnitTest;

public class AsyncSyslogHandlerTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("application-async-syslog.properties", "application.properties"))
            .setLogFileName("AsyncSyslogHandlerTest.log");

    @Test
    public void asyncSyslogHandlerConfigurationTest() throws NullPointerException {
        LogManager logManager = LogManager.getLogManager();
        assertThat(logManager).isInstanceOf(org.jboss.logmanager.LogManager.class);

        DelayedHandler delayedHandler = InitialConfigurator.DELAYED_HANDLER;
        assertThat(Logger.getLogger("").getHandlers()).contains(delayedHandler);
        assertThat(delayedHandler.getLevel()).isEqualTo(Level.ALL);

        Handler handler = Arrays.stream(delayedHandler.getHandlers())
                .filter(h -> (h instanceof AsyncHandler))
                .findFirst().get();
        assertThat(handler).isNotNull();
        assertThat(handler.getLevel()).isEqualTo(Level.ALL);

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
