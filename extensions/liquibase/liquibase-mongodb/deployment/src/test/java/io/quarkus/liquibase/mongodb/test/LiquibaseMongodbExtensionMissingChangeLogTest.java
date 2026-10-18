package io.quarkus.liquibase.mongodb.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that the build succeeds when the Liquibase MongoDB extension is present
 * but no change log file exists (e.g. the dependency is on the classpath
 * without being actively used).
 */
public class LiquibaseMongodbExtensionMissingChangeLogTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("missing-changelog.properties",
                            "application.properties"));

    @Test
    @DisplayName("Build succeeds when no change log file is present")
    public void buildSucceedsWithoutChangeLog() {
    }
}
