package io.quarkus.agroal.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.QuarkusExtensionTest;

public class ValidationFailureFlushThresholdConfigTest {

    @Inject
    AgroalDataSource defaultDataSource;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("base.properties")
            .overrideRuntimeConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:tcp://localhost/mem:flush-threshold")
            .overrideRuntimeConfigKey("quarkus.datasource.jdbc.max-size", "10")
            .overrideRuntimeConfigKey("quarkus.datasource.jdbc.validation-failure-flush-threshold", "3");

    @Test
    public void testValidationFailureFlushThresholdIsApplied() {
        assertThat(defaultDataSource.getConfiguration().connectionPoolConfiguration().validationFailureFlushThreshold())
                .isEqualTo(3);
    }
}
