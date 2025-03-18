package io.quarkus.liquibase.test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InactiveBeanException;
import io.quarkus.liquibase.LiquibaseDataSource;
import io.quarkus.liquibase.LiquibaseFactory;
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
                    "jdbc:h2:tcp://localhost/mem:test-quarkus-migrate-at-start;DB_CLOSE_DELAY=-1");

    @Inject
    @LiquibaseDataSource("users")
    Instance<LiquibaseFactory> liquibase;

    @Test
    @DisplayName("If the URL is missing for the default datasource, even if migrate-at-start is enabled, the application should boot, but Liquibase should be deactivated for that datasource")
    public void testBootSucceedsButLiquibaseDeactivated() {
        assertThatThrownBy(() -> liquibase.get().getConfiguration())
                .isInstanceOf(InactiveBeanException.class)
                .hasMessageContainingAll(
                        "Liquibase for datasource 'users' was deactivated automatically because this datasource was deactivated",
                        "Datasource 'users' was deactivated automatically because its URL is not set.",
                        "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                        "To activate the datasource, set configuration property 'quarkus.datasource.\"users\".jdbc.url'.",
                        "Refer to https://quarkus.io/guides/datasource for guidance.");
    }
}
