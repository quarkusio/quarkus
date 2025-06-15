package io.quarkus.swaggerui.deployment;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class ErroneousConfigTest2 {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setExpectedException(ConfigurationException.class)
            .withApplicationRoot((jar) -> jar.addAsResource(
                    new StringAsset("quarkus.swagger-ui.path=/api\n" + "quarkus.smallrye-openapi.path=/api\n"),
                    "application.properties"));

    @Test
    public void shouldNotStartApplicationIfSwaggerPathIsSameAsOpenAPIPath() {
        Assertions.fail();
    }
}
