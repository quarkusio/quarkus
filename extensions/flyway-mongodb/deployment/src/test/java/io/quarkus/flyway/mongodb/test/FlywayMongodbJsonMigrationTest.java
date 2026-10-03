package io.quarkus.flyway.mongodb.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that JSON-format migrations are applied by the
 * {@code flyway-database-nc-mongodb} native connector when
 * {@code migration-suffixes} is set to {@code .json}.
 * <p>
 * JSON migrations are a single MongoDB command document executed through the
 * driver API and do not require {@code mongosh} to be installed.
 * The connector does not support mixing {@code .js} and {@code .json}
 * migrations within a single project, so this test isolates the JSON
 * migration in its own location.
 */
@ExtendWith(FlapdoodleMongodbExtension.class)
public class FlywayMongodbJsonMigrationTest {

    private static final String DATABASE = "migrate_json";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/migration-json/V1__create_users.json",
                            "db/migration-json/V1__create_users.json"))
            .overrideConfigKey("quarkus.mongodb.connection-string", FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.database", DATABASE)
            .overrideConfigKey("quarkus.flyway-mongodb.database", DATABASE)
            .overrideConfigKey("quarkus.flyway-mongodb.locations", "db/migration-json")
            .overrideConfigKey("quarkus.flyway-mongodb.migration-suffixes", ".json")
            .overrideConfigKey("quarkus.flyway-mongodb.migrate-at-start", "true");

    @Inject
    Flyway flyway;

    @Inject
    MongoClient mongoClient;

    @Test
    void jsonMigrationApplied() {
        MongoCollection<org.bson.Document> users = mongoClient.getDatabase(DATABASE)
                .getCollection("users");
        assertThat(users.countDocuments()).isEqualTo(1L);
        assertThat(users.find().first().getString("name")).isEqualTo("alice");

        MigrationInfoService info = flyway.info();
        assertThat(info.applied()).isNotEmpty();
        boolean v1Found = false;
        for (var migration : info.applied()) {
            if (migration.getVersion() != null
                    && "1".equals(migration.getVersion().getVersion())) {
                v1Found = true;
                break;
            }
        }
        assertThat(v1Found).as("V1 JSON migration should be in applied history").isTrue();
    }
}
