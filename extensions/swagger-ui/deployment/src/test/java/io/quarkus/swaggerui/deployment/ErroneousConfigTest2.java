package io.quarkus.swaggerui.deployment;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.deployment.configuration.ConfigurationError;
import io.quarkus.test.QuarkusUnitTest;

public class ErroneousConfigTest2 {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setExpectedException(ConfigurationError.class)
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset("quarkus.swagger-ui.path=/api\n"
                            + "quarkus.smallrye-openapi.path=/api\n"), "application.properties"));

    @Test
    public void shouldNotStartApplicationIfSwaggerPathIsSameAsOpenAPIPath() {
        Assertions.fail();
    }
}
