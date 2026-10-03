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

/**
 * Pins the behavior of a named MongoDB client that has no explicit
 * {@code quarkus.flyway-mongodb.<name>.*} configuration: a Flyway bean is
 * still produced with default build-time settings (location {@code db/migration},
 * suffix {@code .js}, schema history collection {@code flyway_schema_history})
 * and standard runtime defaults (no migrate-at-start, etc.).
 * <p>
 * This documents that the extension applies sensible defaults to every Mongo
 * client discovered at build time and does not require per-client opt-in for
 * the Flyway bean to be available.
 */
@ExtendWith(FlapdoodleMongodbExtension.class)
public class FlywayMongodbNamedClientNoExplicitConfigTest {

    private static final String DEFAULT_DB = "noexplicit_default";
    private static final String ANALYTICS_DB = "noexplicit_analytics";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addAsResource("db/migration/V1__create_users.json", "db/migration/V1__create_users.json"))
            // Default client: fully configured.
            .overrideConfigKey("quarkus.mongodb.connection-string", FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.database", DEFAULT_DB)
            .overrideConfigKey("quarkus.flyway-mongodb.database", DEFAULT_DB)
            // Named "analytics" client: only the MongoDB side is configured. No
            // quarkus.flyway-mongodb.analytics.* keys at all.
            .overrideConfigKey("quarkus.mongodb.analytics.connection-string",
                    FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.analytics.database", ANALYTICS_DB);

    @Inject
    Flyway defaultFlyway;

    @Inject
    @FlywayMongodbClient("analytics")
    Flyway analyticsFlyway;

    // Inject the named MongoClient so the mongodb-client extension registers
    // "analytics" at build time. Without this, flyway-mongodb has no Mongo client
    // to produce a Flyway bean for, regardless of quarkus.mongodb.analytics.*.
    @Inject
    @MongoClientName("analytics")
    @SuppressWarnings("unused")
    MongoClient analyticsMongoClient;

    @Test
    void unconfiguredNamedClientGetsDefaultFlywayBean() {
        // Default client got its Flyway instance with the user-configured database.
        assertThat(defaultFlyway).isNotNull();

        // Named client with no flyway-mongodb config still has a Flyway bean...
        assertThat(analyticsFlyway).isNotNull();

        // ...wired with the build-time defaults from FlywayMongodbClientBuildTimeConfig.
        assertThat(analyticsFlyway.getConfiguration().getLocations())
                .extracting(Object::toString)
                .containsExactly("classpath:db/migration");
        assertThat(analyticsFlyway.getConfiguration().getSqlMigrationSuffixes())
                .containsExactly(".js");
        // Default schema history collection name (Flyway's default 'table').
        assertThat(analyticsFlyway.getConfiguration().getTable()).isEqualTo("flyway_schema_history");
    }
}
