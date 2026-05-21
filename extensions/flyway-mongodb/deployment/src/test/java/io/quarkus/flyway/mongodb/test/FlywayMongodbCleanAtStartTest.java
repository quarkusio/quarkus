package io.quarkus.flyway.mongodb.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

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
import com.mongodb.client.MongoCollection;

import io.quarkus.test.QuarkusExtensionTest;

@ExtendWith(FlapdoodleMongodbExtension.class)
@ExtendWith(FlywayMongodbCleanAtStartTest.SeedGarbage.class)
public class FlywayMongodbCleanAtStartTest {

    static final String DATABASE = "cleanstart";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/migration/V1__create_users.js",
                            "db/migration/V1__create_users.js"))
            .overrideConfigKey("quarkus.mongodb.connection-string", FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.database", DATABASE)
            .overrideConfigKey("quarkus.flyway-mongodb.database", DATABASE)
            .overrideConfigKey("quarkus.flyway-mongodb.clean-at-start", "true")
            .overrideConfigKey("quarkus.flyway-mongodb.migrate-at-start", "true");

    @Inject
    Flyway flyway;

    @Inject
    MongoClient mongoClient;

    @Test
    void cleanWipesPreExistingStateBeforeMigrate() {
        var db = mongoClient.getDatabase(DATABASE);

        // The garbage_collection seeded before boot must be gone -> proves clean ran.
        assertThat(db.listCollectionNames()).doesNotContain("garbage_collection");

        // users has exactly alice. If clean had not run we'd see 3 (2 stale + alice).
        MongoCollection<Document> users = db.getCollection("users");
        assertThat(users.countDocuments()).isEqualTo(1L);
        Document onlyUser = users.find().first();
        assertThat(onlyUser).isNotNull();
        assertThat(onlyUser.getString("name")).isEqualTo("alice");

        // And the V1 migration ran after clean.
        assertThat(flyway.info().applied())
                .anyMatch(info -> info.getVersion() != null && "1".equals(info.getVersion().getVersion()));
    }

    // Seeds garbage state in the target DB after Mongo starts and before Quarkus boots,
    // so the post-boot assertions can prove clean-at-start actually wiped it.
    static class SeedGarbage implements BeforeAllCallback {
        @Override
        public void beforeAll(ExtensionContext context) {
            try (MongoClient client = MongoClients.create(FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)) {
                var db = client.getDatabase(DATABASE);
                db.drop();
                db.getCollection("users").insertMany(List.of(
                        new Document("name", "stale-1"),
                        new Document("name", "stale-2")));
                db.createCollection("garbage_collection");
            }
        }
    }
}
