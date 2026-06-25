package io.quarkus.flyway.mongodb.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

@ExtendWith(FlapdoodleMongodbExtension.class)
public class FlywayMongodbBaselineAtStartTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/migration/V1__create_users.js",
                            "db/migration/V1__create_users.js"))
            .overrideConfigKey("quarkus.mongodb.connection-string", FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.database", "baseline")
            .overrideConfigKey("quarkus.flyway-mongodb.database", "baseline")
            .overrideConfigKey("quarkus.flyway-mongodb.baseline-at-start", "true")
            .overrideConfigKey("quarkus.flyway-mongodb.baseline-version", "0")
            .overrideConfigKey("quarkus.flyway-mongodb.baseline-description", "<< baseline >>");

    @Inject
    Flyway flyway;

    @Test
    void baselineRecorded() {
        // baseline created an entry; migrate was NOT enabled so V1 is still pending.
        assertThat(flyway.info().applied()).isNotEmpty();
        assertThat(flyway.info().pending()).hasSize(1);
    }
}
