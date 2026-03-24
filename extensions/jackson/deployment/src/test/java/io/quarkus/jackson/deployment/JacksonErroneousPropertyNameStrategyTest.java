package io.quarkus.jackson.deployment;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class JacksonErroneousPropertyNameStrategyTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("application-erroneous-property-name-strategy.properties")
            .setExpectedException(RuntimeException.class);

    @Test
    public void test() {
        fail("Should never have been called");
    }
}
