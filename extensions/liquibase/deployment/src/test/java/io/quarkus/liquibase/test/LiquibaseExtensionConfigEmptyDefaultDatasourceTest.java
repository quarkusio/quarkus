package io.quarkus.liquibase.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.test.QuarkusUnitTest;

public class LiquibaseExtensionConfigEmptyDefaultDatasourceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            // The datasource won't be truly "unconfigured" if dev services are enabled
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .assertException(t -> assertThat(t)
                    .hasMessageContainingAll(
                            "Liquibase for datasource '<default>' was deactivated automatically Liquibase for datasource '<default>' was deactivated automatically because this datasource was not configured",
                            "Datasource '<default>' is not configured.",
                            "To solve this, configure datasource '<default>'.",
                            "Refer to https://quarkus.io/guides/datasource for guidance."));

    @Inject
    LiquibaseFactory liquibaseFactory;

    @Test
    @DisplayName("If there is no config for the default datasource, "
            + "and LiquibaseFactory for the default datasource is injected, the application should fail to boot")
    public void testBootFails() {
        // Should not be reached because boot should fail.
        assertTrue(false);
    }

}
