package io.quarkus.hibernate.validator.test.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.validation.Validator;
import javax.validation.constraints.Max;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ValidConfigurationTest {

    @Inject
    Validator validator;

    @Inject
    ValidNumberConfigService numberConfigService;

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ValidNumberConfigService.class)
                    .addAsResource(new StringAsset("valid-number=5"), "application.properties"));

    @Test
    public void shouldStartSuccessful() {
        assertThat(numberConfigService).isNotNull();
        assertThat(validator.validate(numberConfigService)).isEmpty();
    }

    @Dependent
    public static class ValidNumberConfigService {
        @Max(6)
        @ConfigProperty(name = "valid-number")
        int validNumber;
    }
}
