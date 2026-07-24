package io.quarkus.flyway.mongodb.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;

import jakarta.inject.Inject;

import org.bson.Document;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import io.quarkus.test.QuarkusExtensionTest;

@ExtendWith(FlapdoodleMongodbExtension.class)
@ExtendWith(FlywayMongodbValidateAtStartTest.SeedCorruptHistory.class)
public class FlywayMongodbValidateAtStartTest {

    static final String DATABASE = "validate";
    static final String HISTORY = "flyway_schema_history";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/migration/V1__create_users.json",
                            "db/migration/V1__create_users.json"))
            .overrideConfigKey("quarkus.mongodb.connection-string", FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.database", DATABASE)
            .overrideConfigKey("quarkus.flyway-mongodb.database", DATABASE)
            .overrideConfigKey("quarkus.flyway-mongodb.validate-at-start", "true")
            .overrideConfigKey("quarkus.flyway-mongodb.validate-at-start.clean-on-validation-error", "true")
            .overrideConfigKey("quarkus.flyway-mongodb.migrate-at-start", "true")
            .overrideConfigKey("quarkus.flyway-mongodb.migration-suffixes", ".json");

    @Inject
    Flyway flyway;

    @Inject
    MongoClient mongoClient;

    @Test
    void validateDetectsChecksumMismatchAndCleanOnErrorSelfHeals() {
        var db = mongoClient.getDatabase(DATABASE);

        // Pre-seeded garbage_collection is gone -> clean ran after validate failed.
        assertThat(db.listCollectionNames()).doesNotContain("garbage_collection");

        // Migrate re-applied V1 on the fresh schema.
        var users = db.getCollection("users");
        assertThat(users.countDocuments()).isEqualTo(1L);
        assertThat(users.find().first().getString("name")).isEqualTo("alice");

        // schema_history no longer contains the corrupt checksum.
        assertThat(db.getCollection(HISTORY).countDocuments(new Document("checksum", 99999))).isZero();

        // And info().applied() reflects a fresh V1 entry.
        assertThat(flyway.info().applied())
                .anyMatch(info -> info.getVersion() != null && "1".equals(info.getVersion().getVersion()));
    }

    // Seeds a flyway_schema_history entry with a wrong checksum for V1 so validate-at-start
    // fails, the recorder's clean-on-validation-error branch wipes the DB, and migrate
    // re-applies V1.
    static class SeedCorruptHistory implements BeforeAllCallback {
        @Override
        public void beforeAll(ExtensionContext context) {
            try (MongoClient client = MongoClients.create(FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)) {
                var db = client.getDatabase(DATABASE);
                db.drop();
                db.createCollection("garbage_collection");
                db.getCollection(HISTORY).insertOne(new Document()
                        .append("installed_rank", 1)
                        .append("version", "1")
                        .append("description", "create users")
                        .append("type", "SCRIPT")
                        .append("script", "V1__create_users.json")
                        .append("checksum", 99999)
                        .append("installed_by", "test")
                        .append("installed_on", Timestamp.from(Instant.now()).toString())
                        .append("execution_time", 0)
                        .append("success", true));
            }
        }
    }
}
