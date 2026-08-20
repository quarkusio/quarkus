package io.quarkus.flyway.mongodb.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InactiveBeanException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * When no MongoDB connection string is configured and a bean statically injects Flyway
 * (not Instance), startup must fail with an InactiveBeanException that describes why
 * the MongoDB client was deactivated.
 */
public class FlywayMongodbConnectionStringMissingStaticInjectionTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.mongodb.hosts", "")
            .assertException(e -> assertThat(e)
                    .satisfies(t -> assertThat(t.getClass().getName())
                            .isEqualTo(InactiveBeanException.class.getName()))
                    .hasMessageContainingAll(
                            "Flyway MongoDB for client '<default>' was deactivated automatically because the MongoDB client was deactivated.",
                            "Mongo Client '<default>' was deactivated automatically because neither the hosts nor the connectionString is set"));

    @Inject
    MyBean myBean;

    @Test
    void startupShouldHaveFailed() {
        fail("Startup should have failed");
    }

    @ApplicationScoped
    public static class MyBean {
        @Inject
        Flyway flyway;
    }
}
