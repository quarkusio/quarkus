package io.quarkus.flyway.mongodb.test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InactiveBeanException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * When no MongoDB connection string is configured the MongoDB client is
 * deactivated, which in turn deactivates the Flyway MongoDB bean. The app
 * must still boot and the bean must report a clear deactivation reason.
 */
public class FlywayMongodbConnectionStringMissingTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.flyway-mongodb.migrate-at-start", "true")
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.mongodb.hosts", "");

    @Inject
    Instance<Flyway> flyway;

    @Test
    @DisplayName("App boots but Flyway bean is inactive when MongoDB connection string is absent")
    void flywayBeanInactiveWhenConnectionStringMissing() {
        assertThatThrownBy(flyway::get)
                .isInstanceOf(InactiveBeanException.class)
                .hasMessageContainingAll(
                        "Flyway MongoDB for client '<default>' was deactivated automatically because the MongoDB client was deactivated.",
                        "Mongo Client '<default>' was deactivated automatically because neither the hosts nor the connectionString is set");
    }
}
