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
 * Verifies that a {@code filesystem:} location is passed through to Flyway and
 * migrations on the actual filesystem are discovered and applied correctly.
 */
@ExtendWith(FlapdoodleMongodbExtension.class)
public class FlywayMongodbMigrateAtStartFilesystemPrefixTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.mongodb.connection-string", FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.database", "filesystemprefix")
            .overrideConfigKey("quarkus.flyway-mongodb.locations", "filesystem:src/test/resources/db/migration")
            .overrideConfigKey("quarkus.flyway-mongodb.migrate-at-start", "true")
            .overrideConfigKey("quarkus.flyway-mongodb.database", "filesystemprefix")
            .overrideConfigKey("quarkus.flyway-mongodb.migration-suffixes", ".json");

    @Inject
    Flyway flyway;

    @Inject
    MongoClient mongoClient;

    @Test
    void filesystemLocationMigrationApplied() {
        assertThat(flyway.info().applied()).isNotEmpty();
        assertThat(mongoClient.getDatabase("filesystemprefix").getCollection("users").countDocuments())
                .isEqualTo(1L);
    }
}
