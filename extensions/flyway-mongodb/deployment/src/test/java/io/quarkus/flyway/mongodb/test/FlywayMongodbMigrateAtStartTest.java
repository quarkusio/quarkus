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

@ExtendWith(FlapdoodleMongodbExtension.class)
public class FlywayMongodbMigrateAtStartTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/migration/V1__create_users.js", "db/migration/V1__create_users.js"))
            .overrideConfigKey("quarkus.mongodb.connection-string", FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.database", "migrate")
            .overrideConfigKey("quarkus.flyway-mongodb.migrate-at-start", "true")
            .overrideConfigKey("quarkus.flyway-mongodb.database", "migrate");

    @Inject
    Flyway flyway;

    @Inject
    MongoClient mongoClient;

    @Test
    void migrationApplied() {
        MongoCollection<org.bson.Document> users = mongoClient.getDatabase("migrate")
                .getCollection("users");
        assertThat(users.countDocuments()).isEqualTo(1L);
        assertThat(users.find().first().getString("name")).isEqualTo("alice");

        MigrationInfoService info = flyway.info();
        assertThat(info.applied()).isNotEmpty();
        // Find the V1 entry (baseline rows have null version)
        boolean v1Found = false;
        for (var migration : info.applied()) {
            if (migration.getVersion() != null
                    && "1".equals(migration.getVersion().getVersion())) {
                v1Found = true;
                break;
            }
        }
        assertThat(v1Found).as("V1 migration should be in applied history").isTrue();
    }
}
