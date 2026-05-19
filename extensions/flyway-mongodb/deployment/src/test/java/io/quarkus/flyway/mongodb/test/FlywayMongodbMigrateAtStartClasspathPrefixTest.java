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
 * Verifies that a {@code classpath:} prefix in the locations config is stripped
 * and migrations are discovered and applied correctly.
 */
@ExtendWith(FlapdoodleMongodbExtension.class)
public class FlywayMongodbMigrateAtStartClasspathPrefixTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addAsResource("db/migration/V1__create_users.js",
                            "db/migration-classpath-prefix/V1__create_users.js"))
            .overrideConfigKey("quarkus.mongodb.connection-string", FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.database", "classpathprefix")
            .overrideConfigKey("quarkus.flyway-mongodb.locations", "classpath:db/migration-classpath-prefix")
            .overrideConfigKey("quarkus.flyway-mongodb.migrate-at-start", "true")
            .overrideConfigKey("quarkus.flyway-mongodb.database", "classpathprefix");

    @Inject
    Flyway flyway;

    @Inject
    MongoClient mongoClient;

    @Test
    void classpathPrefixStrippedAndMigrationApplied() {
        assertThat(flyway.info().applied()).isNotEmpty();
        assertThat(mongoClient.getDatabase("classpathprefix").getCollection("users").countDocuments())
                .isEqualTo(1L);
    }
}
