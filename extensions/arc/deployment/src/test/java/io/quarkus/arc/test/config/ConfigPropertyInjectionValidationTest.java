package io.quarkus.arc.test.config;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigPropertyInjectionValidationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Configured.class))
            .setExpectedException(ConfigurationException.class);

    @Test
    public void testValidationFailed() {
        // This method should not be invoked
        Assertions.fail();
    }

    @Unremovable
    @ApplicationScoped
    static class Configured {
        @Inject
        @ConfigProperty(name = "unconfigured")
        String foo;
    }
}
