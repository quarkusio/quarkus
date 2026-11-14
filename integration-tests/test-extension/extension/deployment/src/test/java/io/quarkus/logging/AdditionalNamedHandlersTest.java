package io.quarkus.logging;

import static io.quarkus.logging.LoggingTestsHelper.getHandler;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.extest.runtime.logging.AdditionalLogHandlerValueFactory.TestNamedHandler;
import io.quarkus.test.QuarkusExtensionTest;

public class AdditionalNamedHandlersTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("application-additional-named-handlers.properties")
            .withApplicationRoot((jar) -> jar
                    .addClass(LoggingTestsHelper.class)
                    .addAsManifestResource("application.properties", "microprofile-config.properties"));

    @Test
    public void additionalNamedHandlersConfigurationTest() {
        Logger categoryLogger = LogManager.getLogManager().getLogger("io.quarkus.namedcategory");
        assertThat(categoryLogger).isNotNull();
        assertThat(categoryLogger.getHandlers()).hasSize(1).hasOnlyElementsOfType(TestNamedHandler.class);

        TestNamedHandler categoryHandler = (TestNamedHandler) categoryLogger.getHandlers()[0];
        categoryLogger.info("Logged through the named handler");
        assertThat(categoryHandler.records).extracting("message").contains("Logged through the named handler");

        Handler rootHandler = getHandler(TestNamedHandler.class);
        assertThat(rootHandler).isSameAs(categoryHandler);
    }

}
