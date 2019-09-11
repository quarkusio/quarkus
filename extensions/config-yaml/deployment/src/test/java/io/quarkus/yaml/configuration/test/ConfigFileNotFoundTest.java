package io.quarkus.yaml.configuration.test;

import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.deployment.configuration.ConfigurationError;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigFileNotFoundTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .setExpectedException(ConfigurationError.class);

    @Test
    public void shouldNotBeInvoked() {
        fail("This method should not be invoked");
    }
}
