package io.quarkus.logging.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Formatter;
import java.util.logging.Handler;

import org.jboss.logmanager.handlers.SyslogHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.logging.json.runtime.JsonFormatter;
import io.quarkus.test.QuarkusUnitTest;

public class NamedSyslogJsonFormatterTest extends AbstractNamedJsonFormatterTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-named-syslog-json-formatter.properties");

    @Test
    public void namedJsonHandlerHasJsonFormatter() {
        Handler handler = getNamedHandler("named.syslog.json", SyslogHandler.class);
        assertThat(handler).isNotNull();
        assertThat(handler.getFormatter()).isInstanceOf(JsonFormatter.class);
    }

    @Test
    public void namedPlainHandlerHasPatternFormatter() {
        Handler handler = getNamedHandler("named.syslog.plain", SyslogHandler.class);
        assertThat(handler).isNotNull();
        Formatter formatter = handler.getFormatter();
        assertThat(formatter).isNotInstanceOf(JsonFormatter.class);
    }

}
