package io.quarkus.flyway.mongodb.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;

import io.quarkus.flyway.mongodb.FlywayMongodbClient;
import io.quarkus.mongodb.MongoClientName;
import io.quarkus.test.QuarkusExtensionTest;

@ExtendWith(FlapdoodleMongodbExtension.class)
public class FlywayMongodbMultiClientTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/migration/V1__create_users.json",
                            "db/migration/V1__create_users.json")
                    .addAsResource("analytics-migrations/V1__create_events.json",
                            "analytics-migrations/V1__create_events.json"))
            .overrideConfigKey("quarkus.mongodb.connection-string", FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.database", "appdb")
            .overrideConfigKey("quarkus.mongodb.analytics.connection-string",
                    FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.analytics.database", "analyticsdb")
            .overrideConfigKey("quarkus.flyway-mongodb.migrate-at-start", "true")
            .overrideConfigKey("quarkus.flyway-mongodb.database", "appdb")
            .overrideConfigKey("quarkus.flyway-mongodb.migration-suffixes", ".json")
            .overrideConfigKey("quarkus.flyway-mongodb.analytics.migrate-at-start", "true")
            .overrideConfigKey("quarkus.flyway-mongodb.analytics.database", "analyticsdb")
            .overrideConfigKey("quarkus.flyway-mongodb.analytics.locations", "analytics-migrations")
            .overrideConfigKey("quarkus.flyway-mongodb.analytics.migration-suffixes", ".json");

    @Inject
    Flyway defaultFlyway;

    @Inject
    @FlywayMongodbClient("analytics")
    Flyway analyticsFlyway;

    @Inject
    MongoClient defaultMongoClient;

    @Inject
    @MongoClientName("analytics")
    MongoClient analyticsMongoClient;

    @Test
    void eachClientHasItsOwnMigrations() {
        assertThat(defaultMongoClient.getDatabase("appdb").getCollection("users").countDocuments())
                .isEqualTo(1L);
        assertThat(analyticsMongoClient.getDatabase("analyticsdb").getCollection("events").countDocuments())
                .isEqualTo(1L);

        assertThat(defaultMongoClient.getDatabase("appdb").listCollectionNames())
                .doesNotContain("events");
        assertThat(analyticsMongoClient.getDatabase("analyticsdb").listCollectionNames())
                .doesNotContain("users");

        assertThat(defaultFlyway.info().applied()).isNotEmpty();
        assertThat(analyticsFlyway.info().applied()).isNotEmpty();
    }
}
