package io.quarkus.flyway.mongodb.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

@ExtendWith(FlapdoodleMongodbExtension.class)
public class FlywayMongodbNoMigrateAtStartTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/migration/V1__create_users.json",
                            "db/migration/V1__create_users.json"))
            .overrideConfigKey("quarkus.mongodb.connection-string", FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.database", "nomigrate")
            .overrideConfigKey("quarkus.flyway-mongodb.database", "nomigrate")
            .overrideConfigKey("quarkus.flyway-mongodb.migration-suffixes", ".json");
    // migrate-at-start defaults to false.

    @Inject
    Flyway flyway;

    @Test
    void beanExistsButNoMigrationApplied() {
        assertThat(flyway).isNotNull();
        assertThat(flyway.info().applied()).isEmpty();
        assertThat(flyway.info().pending()).hasSize(1);
    }
}
