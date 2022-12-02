package io.quarkus.liquibase.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.test.QuarkusUnitTest;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSetStatus;

public class LiquibaseExtensionMigrateAtStartYamlChangeLogTest {
    // Quarkus built object
    @Inject
    LiquibaseFactory liquibaseFactory;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/yaml/changeLog.yaml")
                    .addAsResource("db/yaml/create-tables.yaml")
                    .addAsResource("db/yaml/test/test.yaml")
                    .addAsResource("migrate-at-start-yaml-config.properties", "application.properties"));

    @Test
    @DisplayName("Migrates at start with change log config correctly")
    public void testLiquibaseYamlChangeLog() throws Exception {
        try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
            List<ChangeSetStatus> status = liquibase.getChangeSetStatuses(liquibaseFactory.createContexts(),
                    liquibaseFactory.createLabels());
            assertNotNull(status);
            assertEquals(2, status.size());
            assertFalse(status.get(0).getWillRun());
            assertFalse(status.get(1).getWillRun());
        }
    }
}
