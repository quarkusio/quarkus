package io.quarkus.liquibase.mongodb.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that the build succeeds when a {@code classpath:}-prefixed change log
 * is configured but the referenced resource does not exist.
 */
public class LiquibaseMongodbExtensionMissingChangeLogClasspathPrefixTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("missing-changelog-classpath-prefix.properties",
                            "application.properties"));

    @Test
    @DisplayName("Build succeeds when classpath:-prefixed change log is missing")
    public void buildSucceedsWithMissingClasspathPrefixedChangeLog() {
    }
}
