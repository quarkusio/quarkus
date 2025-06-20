package io.quarkus.logging;

import static io.quarkus.logging.LoggingTestsHelper.getHandler;
import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.logmanager.handlers.SyslogHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SyslogCountingFramingTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-syslog-output.properties")
            .overrideConfigKey("quarkus.log.syslog.protocol", "UDP")
            .withApplicationRoot((jar) -> jar.addClass(LoggingTestsHelper.class));

    @Test
    public void syslogOutputTest() {
        SyslogHandler syslogHandler = (SyslogHandler) getHandler(SyslogHandler.class);

        assertThat(syslogHandler.getProtocol()).isEqualTo(SyslogHandler.Protocol.UDP);
        // counting framing is default 'protocol_dependent', and for UDP the counting framing is off
        assertThat(syslogHandler.isUseCountingFraming()).isEqualTo(false);
    }
}
