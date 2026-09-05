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
@ExtendWith(FlywayMongodbRepairAtStartTest.SeedFailedHistory.class)
public class FlywayMongodbRepairAtStartTest {

    static final String DATABASE = "repair";
    static final String HISTORY = "flyway_schema_history";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/migration/V1__create_users.json",
                            "db/migration/V1__create_users.json"))
            .overrideConfigKey("quarkus.mongodb.connection-string", FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.database", DATABASE)
            .overrideConfigKey("quarkus.flyway-mongodb.database", DATABASE)
            .overrideConfigKey("quarkus.flyway-mongodb.repair-at-start", "true")
            .overrideConfigKey("quarkus.flyway-mongodb.migrate-at-start", "true")
            .overrideConfigKey("quarkus.flyway-mongodb.migration-suffixes", ".json");

    @Inject
    Flyway flyway;

    @Inject
    MongoClient mongoClient;

    @Test
    void repairRemovesFailedHistoryEntriesBeforeMigrate() {
        var history = mongoClient.getDatabase(DATABASE).getCollection(HISTORY);

        // No failed entries remain -> proves repair ran.
        assertThat(history.countDocuments(new Document("success", false))).isZero();

        // The seeded failed entry for version 0.5 is gone.
        assertThat(history.countDocuments(new Document("version", "0.5"))).isZero();

        // V1 applied after repair.
        assertThat(flyway.info().applied())
                .anyMatch(info -> info.getVersion() != null && "1".equals(info.getVersion().getVersion()));
    }

    // Seeds a failed migration entry in flyway_schema_history after Mongo starts and before Quarkus boots,
    // so the post-boot assertions can prove repair-at-start removed it.
    static class SeedFailedHistory implements BeforeAllCallback {
        @Override
        public void beforeAll(ExtensionContext context) {
            try (MongoClient client = MongoClients.create(FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)) {
                var db = client.getDatabase(DATABASE);
                db.drop();
                db.getCollection(HISTORY).insertOne(new Document()
                        .append("installed_rank", 1)
                        .append("version", "0.5")
                        .append("description", "failed attempt")
                        .append("type", "SCRIPT")
                        .append("script", "V0_5__failed_attempt.json")
                        .append("checksum", 0)
                        .append("installed_by", "test")
                        .append("installed_on", Timestamp.from(Instant.now()).toString())
                        .append("execution_time", 0)
                        .append("success", false));
            }
        }
    }
}
