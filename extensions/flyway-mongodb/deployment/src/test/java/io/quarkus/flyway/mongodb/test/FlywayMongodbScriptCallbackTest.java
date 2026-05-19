package io.quarkus.flyway.mongodb.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;

import io.quarkus.test.QuarkusExtensionTest;

@ExtendWith(FlapdoodleMongodbExtension.class)
public class FlywayMongodbScriptCallbackTest {

    private static final String DATABASE = "callback";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/migration/V1__create_users.js",
                            "db/migration/V1__create_users.js")
                    .addAsResource("db/callback-extras/V2__insert_bob.js",
                            "db/migration/V2__insert_bob.js")
                    .addAsResource("db/migration/beforeMigrate__marker.js",
                            "db/migration/beforeMigrate__marker.js")
                    .addAsResource("db/migration/afterMigrate__marker.js",
                            "db/migration/afterMigrate__marker.js")
                    .addAsResource("db/migration/beforeEachMigrate__marker.js",
                            "db/migration/beforeEachMigrate__marker.js")
                    .addAsResource("db/migration/afterEachMigrate__marker.js",
                            "db/migration/afterEachMigrate__marker.js"))
            .overrideConfigKey("quarkus.mongodb.connection-string", FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.database", DATABASE)
            .overrideConfigKey("quarkus.flyway-mongodb.database", DATABASE)
            .overrideConfigKey("quarkus.flyway-mongodb.migrate-at-start", "true");

    @Inject
    MongoClient mongoClient;

    private long count(String event) {
        return mongoClient.getDatabase(DATABASE)
                .getCollection("callback_log")
                .countDocuments(new Document("event", event));
    }

    @Test
    void beforeMigrateFiresOncePerMigrateCommand() {
        // 2 pending migrations, but beforeMigrate is a "once" callback.
        assertThat(count("beforeMigrate")).isEqualTo(1L);
    }

    @Test
    void afterMigrateFiresOncePerMigrateCommand() {
        assertThat(count("afterMigrate")).isEqualTo(1L);
    }

    @Test
    void beforeEachMigrateFiresPerPendingMigration() {
        // 2 pending versioned migrations -> 2 invocations.
        assertThat(count("beforeEachMigrate")).isEqualTo(2L);
    }

    @Test
    void afterEachMigrateFiresPerPendingMigration() {
        assertThat(count("afterEachMigrate")).isEqualTo(2L);
    }
}
