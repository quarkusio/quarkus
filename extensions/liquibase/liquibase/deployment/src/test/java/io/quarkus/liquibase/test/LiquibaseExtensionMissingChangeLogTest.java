package io.quarkus.liquibase.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that the build succeeds when the Liquibase extension is present
 * but no change log file exists (e.g. the dependency is on the classpath
 * without being actively used).
 */
public class LiquibaseExtensionMissingChangeLogTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("config-for-default-datasource-without-liquibase.properties",
                            "application.properties"));

    @Test
    @DisplayName("Build succeeds when no change log file is present")
    public void buildSucceedsWithoutChangeLog() {
        // The build itself is the test — if we get here, the deployment
        // processors handled the missing change log gracefully.
    }
}
