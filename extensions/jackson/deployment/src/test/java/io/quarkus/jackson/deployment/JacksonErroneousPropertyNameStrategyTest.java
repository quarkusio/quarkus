package io.quarkus.jackson.deployment;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class JacksonErroneousPropertyNameStrategyTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-erroneous-property-name-strategy.properties")
            .setExpectedException(RuntimeException.class);

    @Test
    public void test() {
        fail("Should never have been called");
    }
}
