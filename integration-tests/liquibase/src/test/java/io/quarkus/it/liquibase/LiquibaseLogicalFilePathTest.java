package io.quarkus.it.liquibase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * JVM coverage for changelogs included with {@code relativeToChangelogFile="true"} where the included
 * file sets {@code logicalFilePath} (see {@code db/xml/test/test.xml}). Native coverage is
 * {@link LiquibaseLogicalFilePathNativeIT}.
 */
@QuarkusTest
@DisplayName("Liquibase logicalFilePath on included changelogs")
public class LiquibaseLogicalFilePathTest {

    @Test
    @DisplayName("Resolves included changelog and runs migrations when logicalFilePath differs from physical path")
    void includedChangeLogWithLogicalFilePathMigratesSuccessfully() {
        LiquibaseFunctionalityTest.doTestLiquibaseQuarkusFunctionality();
    }
}
