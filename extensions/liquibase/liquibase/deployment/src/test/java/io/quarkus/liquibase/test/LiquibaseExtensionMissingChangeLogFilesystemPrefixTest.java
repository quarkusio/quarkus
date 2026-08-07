package io.quarkus.liquibase.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that the build succeeds when a {@code filesystem:}-prefixed change log
 * is configured but the referenced file does not exist.
 */
public class LiquibaseExtensionMissingChangeLogFilesystemPrefixTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("config-for-default-datasource-without-liquibase.properties",
                            "application.properties"))
            .overrideConfigKey("quarkus.liquibase.change-log", "filesystem:/tmp/non-existent-changelog.xml");

    @Test
    @DisplayName("Build succeeds when filesystem:-prefixed change log is missing")
    public void buildSucceedsWithMissingFilesystemPrefixedChangeLog() {
    }
}
