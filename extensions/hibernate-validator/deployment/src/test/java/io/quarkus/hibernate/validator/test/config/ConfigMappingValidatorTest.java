package io.quarkus.hibernate.validator.test.config;

import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.inject.Inject;
import javax.validation.constraints.Max;

import org.eclipse.microprofile.config.Config;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.SmallRyeConfig;

public class ConfigMappingValidatorTest {
    @RegisterExtension
    static final QuarkusUnitTest UNIT_TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset("validator.server.host=localhost\n"), "application.properties"));

    @Inject
    Config config;

    @Test
    void validator() {
        assertThrows(ConfigValidationException.class,
                () -> config.unwrap(SmallRyeConfig.class).getConfigMapping(Server.class),
                "validator.server.host must be less than or equal to 3");
    }

    @ConfigMapping(prefix = "validator.server")
    public interface Server {
        @Max(3)
        String host();
    }
}
