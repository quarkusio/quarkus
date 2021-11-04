package io.quarkus.liquibase.test;

import static org.junit.jupiter.api.Assertions.*;

import javax.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.test.QuarkusUnitTest;
import liquibase.Liquibase;
import liquibase.changelog.DatabaseChangeLog;

public class LiquibaseExtensionLoadChangeLogTest {
    // Quarkus built object
    @Inject
    LiquibaseFactory liquibaseFactory;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/xml/changeLog.xml")
                    .addAsResource("db/xml/create-tables.xml")
                    .addAsResource("db/xml/create-views.xml")
                    .addAsResource("db/xml/test/test.xml")
                    .addAsResource("load-change-log-config.properties", "application.properties"));

    @Test
    @DisplayName("Load the change log config correctly")
    public void testLiquibaseConfigInjection() throws Exception {
        try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
            DatabaseChangeLog changelog = liquibase.getDatabaseChangeLog();
            assertEquals("db/xml/changeLog.xml", changelog.getFilePath());
            assertNotNull(changelog.getChangeSets());
            assertEquals("db/xml/create-tables.xml", changelog.getChangeSets().get(0).getFilePath());
            assertEquals("db/xml/create-views.xml", changelog.getChangeSets().get(1).getFilePath());
            assertEquals("db/xml/test/test.xml", changelog.getChangeSets().get(2).getFilePath());
        }
    }

}
