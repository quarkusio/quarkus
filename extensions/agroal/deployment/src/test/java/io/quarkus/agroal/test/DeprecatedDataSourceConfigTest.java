package io.quarkus.agroal.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class DeprecatedDataSourceConfigTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
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
