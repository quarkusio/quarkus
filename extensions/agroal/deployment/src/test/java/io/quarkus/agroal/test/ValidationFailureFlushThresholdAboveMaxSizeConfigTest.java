package io.quarkus.agroal.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A threshold that is greater than the pool max size can never be reached, so Agroal rejects it.
 * Check that the datasource fails rather than silently ignoring the configured value.
 */
public class ValidationFailureFlushThresholdAboveMaxSizeConfigTest {

    @Inject
    AgroalDataSource defaultDataSource;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("base.properties")
            .overrideRuntimeConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:tcp://localhost/mem:flush-threshold-too-high")
            .overrideRuntimeConfigKey("quarkus.datasource.jdbc.max-size", "10")
            .overrideRuntimeConfigKey("quarkus.datasource.jdbc.validation-failure-flush-threshold", "11")
            .assertException(t -> assertThat(t)
                    .hasStackTraceContaining("Validation failure flush threshold must not be greater than max size"));

    @Test
    public void shouldNotBeInvoked() {
        assertThat(defaultDataSource).isNull();
    }
}
