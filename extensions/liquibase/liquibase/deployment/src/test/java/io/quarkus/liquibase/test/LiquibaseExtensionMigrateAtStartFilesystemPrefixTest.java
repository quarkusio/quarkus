package io.quarkus.liquibase.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * Verifies that a {@code filesystem:}-prefixed change log is resolved and
 * applied correctly at startup.
 */
public class LiquibaseExtensionMigrateAtStartFilesystemPrefixTest {

    private static final Path CHANGELOG_FILE;

    static {
        try {
            Path dir = Files.createTempDirectory("liquibase-test");
            CHANGELOG_FILE = dir.resolve("changeLog.xml");
            Files.writeString(CHANGELOG_FILE,
                    """
                            <?xml version="1.1" encoding="UTF-8" standalone="no"?>
                            <databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                                               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                               xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog https://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">
                                <changeSet author="test" id="fs-1">
                                    <createTable tableName="FS_TEST">
                                        <column name="ID" type="VARCHAR(255)">
                                            <constraints nullable="false"/>
                                        </column>
                                    </createTable>
                                </changeSet>
                            </databaseChangeLog>
                            """);
            CHANGELOG_FILE.toFile().deleteOnExit();
            dir.toFile().deleteOnExit();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Inject
    LiquibaseFactory liquibaseFactory;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("config-for-default-datasource-without-liquibase.properties",
                            "application.properties"))
            .overrideConfigKey("quarkus.liquibase.change-log", "filesystem:" + CHANGELOG_FILE.toAbsolutePath())
            .overrideConfigKey("quarkus.liquibase.migrate-at-start", "true");

    @Test
    @DisplayName("Migrates at start with filesystem:-prefixed change log")
    public void testFilesystemPrefixedChangeLog() throws Exception {
        try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
            List<ChangeSetStatus> status = liquibase.getChangeSetStatuses(liquibaseFactory.createContexts(),
                    liquibaseFactory.createLabels());
            assertNotNull(status);
            assertEquals(1, status.size());
            assertEquals("fs-1", status.get(0).getChangeSet().getId());
            assertFalse(status.get(0).getWillRun());
        }
    }
}
