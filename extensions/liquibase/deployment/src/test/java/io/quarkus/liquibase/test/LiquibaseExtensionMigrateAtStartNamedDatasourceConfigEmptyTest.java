package io.quarkus.liquibase.test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.liquibase.LiquibaseDataSource;
import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.test.QuarkusUnitTest;

public class LiquibaseExtensionMigrateAtStartNamedDatasourceConfigEmptyTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/changeLog.xml", "db/changeLog.xml"))
            .overrideConfigKey("quarkus.liquibase.users.migrate-at-start", "true")
            // The datasource won't be truly "unconfigured" if dev services are enabled
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            // We need this otherwise it's going to be the *default* datasource making everything fail
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.username", "sa")
            .overrideConfigKey("quarkus.datasource.password", "sa")
            .overrideConfigKey("quarkus.datasource.jdbc.url",
                    "jdbc:h2:tcp://localhost/mem:test-quarkus-migrate-at-start;DB_CLOSE_DELAY=-1");

    @Inject
    @LiquibaseDataSource("users")
    Instance<LiquibaseFactory> liquibaseForNamedDatasource;

    @Test
    @DisplayName("If there is no config for a named datasource, even if migrate-at-start is enabled, the application should boot, but Liquibase should be deactivated for that datasource")
    public void testBootSucceedsButLiquibaseDeactivated() {
        assertThatThrownBy(() -> liquibaseForNamedDatasource.get().getConfiguration())
                .isInstanceOf(UnsatisfiedResolutionException.class)
                .hasMessageContaining("No bean found");
    }
}
