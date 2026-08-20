package io.quarkus.flyway.mongodb.test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

@ExtendWith(FlapdoodleMongodbExtension.class)
public class FlywayMongodbCleanDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/migration/V1__create_users.json",
                            "db/migration/V1__create_users.json"))
            .overrideConfigKey("quarkus.mongodb.connection-string", FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.database", "cleandisabled")
            .overrideConfigKey("quarkus.flyway-mongodb.database", "cleandisabled")
            .overrideConfigKey("quarkus.flyway-mongodb.migrate-at-start", "true")
            .overrideConfigKey("quarkus.flyway-mongodb.clean-disabled", "true")
            .overrideConfigKey("quarkus.flyway-mongodb.migration-suffixes", ".json");

    @Inject
    Flyway flyway;

    @Test
    void cleanRejectedByFlywayWhenCleanDisabledIsTrue() {
        assertThatThrownBy(() -> flyway.clean())
                .isInstanceOf(FlywayException.class);
    }
}
