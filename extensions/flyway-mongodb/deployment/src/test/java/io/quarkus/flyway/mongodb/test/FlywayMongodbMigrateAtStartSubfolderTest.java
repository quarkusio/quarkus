package io.quarkus.flyway.mongodb.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that migration files placed in a subdirectory of the configured
 * location are discovered and applied.
 */
@ExtendWith(FlapdoodleMongodbExtension.class)
public class FlywayMongodbMigrateAtStartSubfolderTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addAsResource("db/migration/V1__create_users.json",
                            "db/migration-subfolder/subfolder/V1__create_users.json"))
            .overrideConfigKey("quarkus.mongodb.connection-string", FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.database", "subfolder")
            .overrideConfigKey("quarkus.flyway-mongodb.locations", "db/migration-subfolder")
            .overrideConfigKey("quarkus.flyway-mongodb.migrate-at-start", "true")
            .overrideConfigKey("quarkus.flyway-mongodb.database", "subfolder")
            .overrideConfigKey("quarkus.flyway-mongodb.migration-suffixes", ".json");

    @Inject
    Flyway flyway;

    @Inject
    MongoClient mongoClient;

    @Test
    void migrationInSubfolderApplied() {
        assertThat(flyway.info().applied()).isNotEmpty();
        assertThat(mongoClient.getDatabase("subfolder").getCollection("users").countDocuments())
                .isEqualTo(1L);
    }
}
