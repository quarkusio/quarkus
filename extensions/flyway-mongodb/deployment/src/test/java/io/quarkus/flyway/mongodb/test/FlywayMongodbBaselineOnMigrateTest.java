package io.quarkus.flyway.mongodb.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that baseline-on-migrate automatically inserts a baseline entry into the
 * schema history when migrate runs against a non-empty database with no schema history.
 */
@ExtendWith(FlapdoodleMongodbExtension.class)
public class FlywayMongodbBaselineOnMigrateTest {

    private static final String DATABASE = "baselineonmigrate";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addAsResource("db/migration/V1__create_users.js", "db/migration/V1__create_users.js"))
            .overrideConfigKey("quarkus.mongodb.connection-string", FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.database", DATABASE)
            .overrideConfigKey("quarkus.flyway-mongodb.database", DATABASE)
            .overrideConfigKey("quarkus.flyway-mongodb.baseline-on-migrate", "true")
            .overrideConfigKey("quarkus.flyway-mongodb.baseline-version", "0.0.1")
            .overrideConfigKey("quarkus.flyway-mongodb.baseline-description", "Initial description for test");

    @Inject
    Flyway flyway;

    @Inject
    MongoClient mongoClient;

    @Test
    @DisplayName("baseline-on-migrate creates a baseline entry when migrating against a non-empty database")
    void baselineOnMigrateCreatesBaselineEntry() {
        // baseline-on-migrate only triggers when the schema is non-empty and has no schema history collection.
        // Pre-populate with a dummy collection so Flyway's migrate detects an existing schema and inserts a baseline.
        mongoClient.getDatabase(DATABASE).createCollection("preexisting");

        flyway.migrate();

        MigrationInfo[] applied = flyway.info().applied();
        assertThat(applied).isNotEmpty();

        assertThat(applied)
                .as("baseline entry with version 0.0.1 and description 'Initial description for test' should be present")
                .anyMatch(info -> info.getVersion() != null
                        && "0.0.1".equals(info.getVersion().getVersion())
                        && "Initial description for test".equals(info.getDescription()));
    }
}
