package io.quarkus.liquibase.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.liquibase.LiquibaseDataSource;
import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.test.QuarkusUnitTest;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSetStatus;

public class LiquibaseExtensionBaselineOnMigrateNamedDataSourceTest {

    @Inject
    @LiquibaseDataSource("users")
    LiquibaseFactory liquibaseFactory;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/changeLog.xml")
                    .addAsResource("baseline-on-migrate-named-datasource.properties", "application.properties"));

    @Test
    @DisplayName("Create history table correctly")
    public void testLiquibaseInitialBaselineInfo() throws Exception {
        try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
            List<ChangeSetStatus> status = liquibase.getChangeSetStatuses(liquibaseFactory.createContexts(),
                    liquibaseFactory.createLabels());
            assertNotNull(status, "Status is null");
            assertEquals(1, status.size(), "The set of changes is not null");
            assertFalse(status.get(0).getWillRun());
        }
    }
}
