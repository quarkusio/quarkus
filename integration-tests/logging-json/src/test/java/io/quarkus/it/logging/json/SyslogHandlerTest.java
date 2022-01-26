package io.quarkus.it.logging.json;

import io.quarkus.test.QuarkusUnitTest;
import org.jboss.logmanager.formatters.JsonFormatter;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.SyslogHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;

import static io.quarkus.it.logging.json.LoggingTestsHelper.getHandler;;
import static org.assertj.core.api.Assertions.assertThat;
import static org.wildfly.common.net.HostName.getQualifiedHostName;
import static org.wildfly.common.os.Process.getProcessName;

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
        assertThat(formatter).isInstanceOf(JsonFormatter.class);

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
