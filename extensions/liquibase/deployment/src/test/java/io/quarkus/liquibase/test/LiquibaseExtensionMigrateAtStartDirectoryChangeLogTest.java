package io.quarkus.liquibase.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.test.QuarkusUnitTest;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSetStatus;

public class LiquibaseExtensionMigrateAtStartDirectoryChangeLogTest {
    // Quarkus built object
    @Inject
    LiquibaseFactory liquibaseFactory;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/all/changeLog.xml")
                    .addAsResource("db/all/test/changeLog.sql")
                    .addAsResource("migrate-at-start-directory-config.properties", "application.properties"));

    @Test
    @DisplayName("Migrates at start with change log config correctly")
    public void testLiquibaseSqlChangeLog() throws Exception {
        try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
            List<ChangeSetStatus> status = liquibase.getChangeSetStatuses(liquibaseFactory.createContexts(),
                    liquibaseFactory.createLabels());
            assertNotNull(status);
            assertEquals(2, status.size());
            assertFalse(status.get(0).getWillRun());
            assertEquals(status.get(0).getChangeSet().getId(), "create-tables-1");
            assertFalse(status.get(1).getWillRun());
            assertEquals(status.get(1).getChangeSet().getId(), "test-1");
        }
    }
}
