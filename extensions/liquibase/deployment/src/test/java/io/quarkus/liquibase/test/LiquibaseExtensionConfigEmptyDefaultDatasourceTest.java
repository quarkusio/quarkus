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
            .assertException(t -> assertThat(t).cause().cause()
                    .hasMessageContainingAll("Unable to find datasource '<default>' for Liquibase",
                            "Datasource '<default>' is not configured.",
                            "To solve this, configure datasource '<default>'.",
                            "Refer to https://quarkus.io/guides/datasource for guidance."));

    @Test
    @DisplayName("If there is no config for the default datasource, the application should fail to boot")
    public void testBootFails() {
        // Should not be reached because boot should fail.
        assertTrue(false);
    }

}
