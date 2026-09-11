package io.quarkus.flyway.mongodb.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;

import io.quarkus.arc.InactiveBeanException;
import io.quarkus.flyway.mongodb.FlywayMongodbClient;
import io.quarkus.mongodb.MongoClientName;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * When a named MongoDB client has no connection string and a bean statically injects
 * the named Flyway bean (not Instance), startup must fail with an InactiveBeanException
 * that describes why the named MongoDB client was deactivated.
 */
@ExtendWith(FlapdoodleMongodbExtension.class)
public class FlywayMongodbNamedClientConnectionStringMissingStaticInjectionTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.mongodb.connection-string", FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.database", "namedmissingstatic")
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            // @MongoClientName("analytics") below causes the analytics client to be registered at build time.
            // Empty hosts deactivates it at runtime so the Flyway bean becomes auto-inactive.
            .overrideConfigKey("quarkus.mongodb.analytics.hosts", "")
            .assertException(e -> assertThat(e)
                    .satisfies(t -> assertThat(t.getClass().getName())
                            .isEqualTo(InactiveBeanException.class.getName()))
                    .hasMessageContainingAll(
                            "Flyway MongoDB for client 'analytics' was deactivated automatically because the MongoDB client was deactivated.",
                            "Mongo Client 'analytics' was deactivated automatically because neither the hosts nor the connectionString is set"));

    @Inject
    MyBean myBean;

    // @MongoClientName("analytics") annotation causes the analytics MongoDB client to be
    // registered at build time by the MongoDB processor, which in turn causes the Flyway
    // processor to create an analytics Flyway bean (auto-inactive when the MongoDB client
    // has no hosts/connectionString).
    @Inject
    @MongoClientName("analytics")
    Instance<MongoClient> analyticsMongoClient;

    @Test
    void startupShouldHaveFailed() {
        fail("Startup should have failed");
    }

    @ApplicationScoped
    public static class MyBean {
        @Inject
        @FlywayMongodbClient("analytics")
        Flyway flyway;
    }
}
