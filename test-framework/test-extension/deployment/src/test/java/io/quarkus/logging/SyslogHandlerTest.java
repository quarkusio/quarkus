package io.quarkus.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.wildfly.common.net.HostName.getQualifiedHostName;
import static org.wildfly.common.os.Process.getProcessName;

import java.util.Arrays;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.DelayedHandler;
import org.jboss.logmanager.handlers.SyslogHandler;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.logging.InitialConfigurator;
import io.quarkus.test.QuarkusUnitTest;

public class SyslogHandlerTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("application-syslog-output.properties", "application.properties"));

    @Test
    public void syslogOutputTest() {
        LogManager logManager = LogManager.getLogManager();
        assertThat(logManager).isInstanceOf(org.jboss.logmanager.LogManager.class);

        DelayedHandler delayedHandler = InitialConfigurator.DELAYED_HANDLER;
        assertThat(Logger.getLogger("").getHandlers()).contains(delayedHandler);

        Handler handler = Arrays.stream(delayedHandler.getHandlers()).filter(h -> (h instanceof SyslogHandler))
                .findFirst().get();
        assertThat(handler).isNotNull();
        assertThat(handler.getLevel()).isEqualTo(Level.WARNING);

        Formatter formatter = handler.getFormatter();
        assertThat(formatter).isInstanceOf(PatternFormatter.class);
        PatternFormatter patternFormatter = (PatternFormatter) formatter;
        assertThat(patternFormatter.getPattern()).isEqualTo("%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n");

        SyslogHandler syslogHandler = (SyslogHandler) handler;
        assertThat(syslogHandler.getPort()).isEqualTo(5140);
        assertThat(syslogHandler.getAppName()).isEqualTo(getProcessName());
        assertThat(syslogHandler.getHostname()).isEqualTo(getQualifiedHostName());
        assertThat(syslogHandler.getFacility()).isEqualTo(SyslogHandler.Facility.USER_LEVEL);
        assertThat(syslogHandler.getSyslogType()).isEqualTo(SyslogHandler.SyslogType.RFC5424);
        assertThat(syslogHandler.getProtocol()).isEqualTo(SyslogHandler.Protocol.TCP);
        assertThat(syslogHandler.isUseCountingFraming()).isEqualTo(false);
        assertThat(syslogHandler.isTruncate()).isEqualTo(true);
        assertThat(syslogHandler.isBlockOnReconnect()).isEqualTo(false);
    }
}
