package io.quarkus.logging;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class CategoryConfiguredHandlerInvalidDueToMultipleHandlersTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setExpectedException(RuntimeException.class)
            .withConfigurationResource("application-category-invalid-configured-handlers-output.properties")
            .withApplicationRoot((jar) -> jar
                    .addAsManifestResource("application.properties", "microprofile-config.properties"));

    @Test
    public void consoleOutputTest() {
        fail("This method should not be invoked because of invalid configuration of logging handlers");
    }

}
