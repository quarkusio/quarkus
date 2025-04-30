package io.quarkus.liquibase.test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InactiveBeanException;
import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.test.QuarkusUnitTest;

public class LiquibaseExtensionMigrateAtStartDefaultDatasourceConfigActiveFalseTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/changeLog.xml", "db/changeLog.xml"))
            .overrideConfigKey("quarkus.datasource.active", "false")
            .overrideConfigKey("quarkus.liquibase.migrate-at-start", "true");

    @Inject
    Instance<LiquibaseFactory> liquibase;

    @Test
    @DisplayName("If the default datasource is deactivated, even if migrate-at-start is enabled, the application should boot, but Liquibase should be deactivated for that datasource")
    public void testBootSucceedsButLiquibaseDeactivated() {
        assertThatThrownBy(() -> liquibase.get().getConfiguration())
                .isInstanceOf(InactiveBeanException.class)
                .hasMessageContainingAll(
                        "Liquibase for datasource '<default>' was deactivated automatically because this datasource was deactivated",
                        "Datasource '<default>' was deactivated through configuration properties.",
                        "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                        "To activate the datasource, set configuration property 'quarkus.datasource.active'"
                                + " to 'true' and configure datasource '<default>'",
                        "Refer to https://quarkus.io/guides/datasource for guidance.");
    }

}
