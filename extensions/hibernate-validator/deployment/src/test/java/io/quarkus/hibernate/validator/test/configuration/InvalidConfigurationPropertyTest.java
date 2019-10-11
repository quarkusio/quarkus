package io.quarkus.hibernate.validator.test.configuration;

import static org.assertj.core.api.Assertions.fail;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.validation.ConstraintViolationException;
import javax.validation.constraints.Max;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class InvalidConfigurationPropertyTest {
    @Inject
    InvalidNumberConfigService numberConfigService;

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setExpectedException(ConstraintViolationException.class)
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(InvalidNumberConfigService.class)
                    .addAsResource(new StringAsset("invalid-number=10"), "application.properties"));

    @Test
    public void shouldFail() {
        fail("should not reach here because configuration value is not valid");
    }

    @Dependent
    public static class InvalidNumberConfigService {
        @Max(4)
        @ConfigProperty(name = "invalid-number")
        int invalidNumber;
    }
}
