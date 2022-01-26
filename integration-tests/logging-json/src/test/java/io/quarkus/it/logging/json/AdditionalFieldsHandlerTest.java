package io.quarkus.it.logging.json;

import io.quarkus.logging.json.runtime.LoggingJsonRecorder;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.logmanager.formatters.JsonFormatter;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.ConsoleHandler;
import org.jboss.logmanager.handlers.SocketHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;

import static io.quarkus.it.logging.json.LoggingTestsHelper.getHandler;
import static org.assertj.core.api.Assertions.assertThat;


public class AdditionalFieldsHandlerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-additional-fields.properties")
            .withApplicationRoot((jar) -> jar
                    .addClass(LoggingTestsHelper.class)
                    .addAsManifestResource("application.properties", "microprofile-config.properties"));

    @Test
    public void additionalFieldsHandlerTest() {
        Handler handler = getHandler(SocketHandler.class);
        assertThat(handler.getLevel()).isEqualTo(Level.WARNING);

        Formatter formatter = handler.getFormatter();
        assertThat(formatter).isInstanceOf(JsonFormatter.class);
        LoggingJsonRecorder.CustomFieldsJsonFormatter customFieldsJsonFormatter = (LoggingJsonRecorder.CustomFieldsJsonFormatter) formatter;
        assertThat(customFieldsJsonFormatter.additionalFields).containsKey("myownfield");
        assertThat(customFieldsJsonFormatter.additionalFields.get("myownfield")).isEqualTo("testingvalue");
        SocketHandler socketHandler = (SocketHandler) handler;
        assertThat(socketHandler.getPort()).isEqualTo(5140);
        assertThat(socketHandler.getAddress().getHostAddress()).isEqualTo("127.0.0.1");
        assertThat(socketHandler.getProtocol()).isEqualTo(SocketHandler.Protocol.TCP);
        assertThat(socketHandler.isBlockOnReconnect()).isEqualTo(false);
        Handler consoleHanlder = getHandler(ConsoleHandler.class);
        assertThat(consoleHanlder).isNotNull();
        formatter = consoleHanlder.getFormatter();
        assertThat(formatter).isInstanceOf(PatternFormatter.class);
    }
}
