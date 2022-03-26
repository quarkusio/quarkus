package io.quarkus.logging;

import static io.quarkus.logging.LoggingTestsHelper.getHandler;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Handler;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.extest.runtime.logging.AdditionalLogHandlerValueFactory.TestHandler;
import io.quarkus.test.QuarkusUnitTest;

public class AdditionalHandlersTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-additional-handlers.properties")
            .withApplicationRoot((jar) -> jar
                    .addClass(LoggingTestsHelper.class)
                    .addAsManifestResource("application.properties", "microprofile-config.properties"));

    @Test
    public void additionalHandlersConfigurationTest() {
        Handler handler = getHandler(TestHandler.class);
        assertThat(handler.getLevel()).isEqualTo(Level.FINE);

        TestHandler testHandler = (TestHandler) handler;
        assertThat(testHandler.records).isNotEmpty();
    }

}
