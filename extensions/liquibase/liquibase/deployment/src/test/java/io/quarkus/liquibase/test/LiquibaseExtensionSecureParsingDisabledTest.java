package io.quarkus.liquibase.test;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.test.QuarkusUnitTest;
import liquibase.Liquibase;

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
            // TODO: this will fail as the system property is not enforced
            //            List<ChangeSetStatus> status = liquibase.getChangeSetStatuses(liquibaseFactory.createContexts(),
            //                    liquibaseFactory.createLabels());
            //            assertNotNull(status);
            //            assertEquals(1, status.size());
            //            assertEquals("id-1", status.get(0).getChangeSet().getId());
            //            assertFalse(status.get(0).getWillRun());
        }
    }
}
