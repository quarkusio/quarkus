package io.quarkus.liquibase.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class LiquibaseExtensionMigrateAtStartNamedDatasourceConfigUrlMissingTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/changeLog.xml", "db/changeLog.xml"))
            .overrideConfigKey("quarkus.liquibase.users.migrate-at-start", "true")
            // The URL won't be missing if dev services are enabled
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            // We need at least one build-time property for the datasource,
            // otherwise it's considered unconfigured at build time...
            .overrideConfigKey("quarkus.datasource.users.db-kind", "h2")
            // We need this otherwise it's going to be the *default* datasource making everything fail
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.username", "sa")
            .overrideConfigKey("quarkus.datasource.password", "sa")
            .overrideConfigKey("quarkus.datasource.jdbc.url",
                    "jdbc:h2:tcp://localhost/mem:test-quarkus-migrate-at-start;DB_CLOSE_DELAY=-1")
            .assertException(t -> assertThat(t).cause().cause()
                    .hasMessageContainingAll("Unable to find datasource 'users' for Liquibase",
                            "Datasource 'users' is not configured.",
                            "To solve this, configure datasource 'users'.",
                            "Refer to https://quarkus.io/guides/datasource for guidance."));

    @Test
    @DisplayName("If the URL is missing for a named datasource, and if migrate-at-start is enabled, the application should fail to boot")
    public void testBootFails() {
        // Should not be reached because boot should fail.
        assertTrue(false);
    }
}
