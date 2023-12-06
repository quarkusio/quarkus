package io.quarkus.liquibase.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class LiquibaseExtensionConfigEmptyDefaultDatasourceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            // The datasource won't be truly "unconfigured" if dev services are enabled
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .assertException(t -> assertThat(t).rootCause()
                    .hasMessageContaining("No datasource has been configured"));

    @Test
    @DisplayName("If there is no config for the default datasource, the application should fail to boot")
    public void testBootFails() {
        // Should not be reached because boot should fail.
        assertTrue(false);
    }

}
