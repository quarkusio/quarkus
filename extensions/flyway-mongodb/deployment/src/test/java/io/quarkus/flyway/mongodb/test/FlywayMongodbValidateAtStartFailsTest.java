package io.quarkus.flyway.mongodb.test;

import static org.assertj.core.api.Assertions.fail;

import java.sql.Timestamp;
import java.time.Instant;

import org.bson.Document;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * Validate-at-start without clean-on-validation-error must abort Quarkus startup
 * with a FlywayValidateException when the schema history is corrupt.
 */
@ExtendWith(FlapdoodleMongodbExtension.class)
@ExtendWith(FlywayMongodbValidateAtStartFailsTest.SeedCorruptHistory.class)
public class FlywayMongodbValidateAtStartFailsTest {

    static final String DATABASE = "validatefails";
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
            .overrideConfigKey("quarkus.flyway-mongodb.validate-at-start.clean-on-validation-error", "false")
            .overrideConfigKey("quarkus.flyway-mongodb.migrate-at-start", "true")
            .overrideConfigKey("quarkus.flyway-mongodb.migration-suffixes", ".json")
            .setExpectedException(FlywayValidateException.class);

    @Test
    void startupShouldHaveFailed() {
        fail("Startup should have failed");
    }

    // Seeds a corrupt V1 entry in flyway_schema_history so validate-at-start fails.
    static class SeedCorruptHistory implements BeforeAllCallback {
        @Override
        public void beforeAll(ExtensionContext context) {
            try (MongoClient client = MongoClients.create(FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)) {
                var db = client.getDatabase(DATABASE);
                db.drop();
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
