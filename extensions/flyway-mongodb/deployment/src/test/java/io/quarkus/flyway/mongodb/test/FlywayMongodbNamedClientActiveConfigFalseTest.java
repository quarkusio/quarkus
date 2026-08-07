package io.quarkus.flyway.mongodb.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;

import io.quarkus.arc.InactiveBeanException;
import io.quarkus.flyway.mongodb.FlywayMongodbClient;
import io.quarkus.mongodb.MongoClientName;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * When a named flyway-mongodb client is explicitly deactivated the default
 * client continues to work, and only the named client's bean is inactive.
 */
@ExtendWith(FlapdoodleMongodbExtension.class)
public class FlywayMongodbNamedClientActiveConfigFalseTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addAsResource("db/migration/V1__create_users.json", "db/migration/V1__create_users.json"))
            .overrideConfigKey("quarkus.mongodb.connection-string", FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.database", "namedacf")
            .overrideConfigKey("quarkus.flyway-mongodb.migrate-at-start", "true")
            .overrideConfigKey("quarkus.flyway-mongodb.database", "namedacf")
            .overrideConfigKey("quarkus.flyway-mongodb.migration-suffixes", ".json")
            .overrideConfigKey("quarkus.mongodb.analytics.connection-string",
                    FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.analytics.database", "namedacf_analytics")
            .overrideConfigKey("quarkus.flyway-mongodb.analytics.active", "false");

    @Inject
    Flyway defaultFlyway;

    @Inject
    @MongoClientName("analytics")
    MongoClient analyticsMongoClient;

    @Inject
    @FlywayMongodbClient("analytics")
    Instance<Flyway> analyticsFlyway;

    @Test
    @DisplayName("Default Flyway bean is active; analytics Flyway bean is inactive")
    void defaultActiveNamedInactive() {
        assertThat(defaultFlyway.info().applied()).isNotEmpty();

        assertThatThrownBy(analyticsFlyway::get)
                .isInstanceOf(InactiveBeanException.class)
                .hasMessageContainingAll(
                        "Flyway MongoDB for client 'analytics' was deactivated through configuration.",
                        "quarkus.flyway-mongodb.analytics.active");
    }
}
