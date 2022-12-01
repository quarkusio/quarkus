package io.quarkus.agroal.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class DeprecatedDataSourceConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-deprecated-config.properties")
            .assertException(e -> {
                assertThat(e).isInstanceOf(ConfigurationException.class);
                assertThat(e)
                        .hasMessageStartingWith("quarkus.datasource.url and quarkus.datasource.driver have been deprecated");
            });

    @Test
    public void deprecatedConfigThrowsException() {
        // Should not be reached: verify
        assertTrue(false);
    }

}
