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
 * When a named MongoDB client has no connection string its flyway-mongodb bean
 * is deactivated automatically, while the default client continues to work.
 */
@ExtendWith(FlapdoodleMongodbExtension.class)
public class FlywayMongodbNamedClientConnectionStringMissingTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addAsResource("db/migration/V1__create_users.js", "db/migration/V1__create_users.js"))
            .overrideConfigKey("quarkus.mongodb.connection-string", FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.database", "namedmissing")
            .overrideConfigKey("quarkus.flyway-mongodb.migrate-at-start", "true")
            .overrideConfigKey("quarkus.flyway-mongodb.database", "namedmissing")
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.mongodb.analytics.hosts", "");

    @Inject
    Flyway defaultFlyway;

    @Inject
    @MongoClientName("analytics")
    Instance<MongoClient> analyticsMongoClient;

    @Inject
    @FlywayMongodbClient("analytics")
    Instance<Flyway> analyticsFlyway;

    @Test
    @DisplayName("Default Flyway bean is active; analytics Flyway bean is inactive due to missing connection string")
    void defaultActiveNamedInactiveDueToMissingConnectionString() {
        assertThat(defaultFlyway.info().applied()).isNotEmpty();

        assertThatThrownBy(analyticsFlyway::get)
                .isInstanceOf(InactiveBeanException.class)
                .hasMessageContainingAll(
                        "Flyway MongoDB for client 'analytics' was deactivated automatically because the MongoDB client was deactivated.",
                        "Mongo Client 'analytics' was deactivated automatically because neither the hosts nor the connectionString is set");
    }
}
