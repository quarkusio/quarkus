package io.quarkus.logging;

import static io.quarkus.logging.LoggingTestsHelper.getHandler;
import static org.assertj.core.api.Assertions.assertThat;
import static org.wildfly.common.net.HostName.getQualifiedHostName;
import static org.wildfly.common.os.Process.getProcessName;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.SyslogHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SyslogHandlerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-syslog-output.properties")
            .withApplicationRoot((jar) -> jar
                    .addClass(LoggingTestsHelper.class)
                    .addAsManifestResource("application.properties", "microprofile-config.properties"));

    @Test
    public void syslogOutputTest() {
        Handler handler = getHandler(SyslogHandler.class);
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
