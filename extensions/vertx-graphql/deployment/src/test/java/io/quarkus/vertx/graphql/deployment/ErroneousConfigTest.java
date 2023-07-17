package io.quarkus.vertx.graphql.deployment;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class ErroneousConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setExpectedException(ConfigurationException.class)
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.vertx-graphql.ui.path=/\n"), "application.properties"));

    @Test
    public void shouldNotStartApplicationIfPathIsASlash() {
        Assertions.fail();
    }
}
