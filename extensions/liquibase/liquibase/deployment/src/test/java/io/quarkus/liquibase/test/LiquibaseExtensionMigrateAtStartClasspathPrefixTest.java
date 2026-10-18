package io.quarkus.liquibase.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.test.QuarkusExtensionTest;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSetStatus;

/**
 * Verifies that a {@code classpath:}-prefixed change log is resolved and
 * applied correctly at startup.
 */
public class LiquibaseExtensionMigrateAtStartClasspathPrefixTest {

    @Inject
    LiquibaseFactory liquibaseFactory;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/changeLog.xml", "db/changeLog.xml")
                    .addAsResource("classpath-prefix-config.properties", "application.properties"));

    @Test
    @DisplayName("Migrates at start with classpath:-prefixed change log")
    public void testClasspathPrefixedChangeLog() throws Exception {
        try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
            List<ChangeSetStatus> status = liquibase.getChangeSetStatuses(liquibaseFactory.createContexts(),
                    liquibaseFactory.createLabels());
            assertNotNull(status);
            assertEquals(1, status.size());
            assertEquals("id-1", status.get(0).getChangeSet().getId());
            assertFalse(status.get(0).getWillRun());
        }
    }
}
