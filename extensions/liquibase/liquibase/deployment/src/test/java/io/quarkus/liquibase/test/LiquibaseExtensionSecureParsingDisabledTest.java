package io.quarkus.liquibase.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.test.QuarkusUnitTest;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSetStatus;

public class LiquibaseExtensionSecureParsingDisabledTest {

    @Inject
    LiquibaseFactory liquibaseFactory;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("insecure-db/changeLog.xml", "db/changeLog.xml")
                    .addAsResource("insecure-db/dbchangelog-3.8.xsd", "db/dbchangelog-3.8.xsd")
                    .addAsResource("secure-parsing-disabled.properties", "application.properties"));

    @Test
    public void testSecureParsingDisabled() throws Exception {
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
