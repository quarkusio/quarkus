package io.quarkus.smallrye.graphql.deployment.ui;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class ErroneousConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setExpectedException(ConfigurationException.class)
            .withApplicationRoot((jar) -> jar.addAsResource(
                    new StringAsset("quarkus.smallrye-graphql.ui.root-path=/\n"), "application.properties"));

    @Test
    public void shouldNotStartApplicationIfUIPathIsASlash() {
        Assertions.fail();
    }
}
