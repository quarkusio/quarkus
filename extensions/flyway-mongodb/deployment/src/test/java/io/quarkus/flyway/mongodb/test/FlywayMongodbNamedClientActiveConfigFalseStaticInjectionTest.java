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
 * When a named flyway-mongodb client is explicitly deactivated and a bean statically injects
 * the named Flyway bean (not Instance), startup must fail with an InactiveBeanException that
 * names the config property to use for activation.
 */
@ExtendWith(FlapdoodleMongodbExtension.class)
public class FlywayMongodbNamedClientActiveConfigFalseStaticInjectionTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.mongodb.connection-string", FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.database", "namedacfstatic")
            // Build-time property needed so the MongoDB processor registers the analytics client
            // and the Flyway processor creates the analytics Flyway bean.
            .overrideConfigKey("quarkus.mongodb.analytics.connection-string",
                    FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.analytics.database", "namedacfstatic_analytics")
            .overrideConfigKey("quarkus.flyway-mongodb.analytics.active", "false")
            .assertException(e -> assertThat(e)
                    .satisfies(t -> assertThat(t.getClass().getName())
                            .isEqualTo(InactiveBeanException.class.getName()))
                    .hasMessageContainingAll(
                            "Flyway MongoDB for client 'analytics' was deactivated through configuration.",
                            "quarkus.flyway-mongodb.analytics.active"));

    @Inject
    MyBean myBean;

    // @MongoClientName("analytics") annotation causes the analytics MongoDB client to be
    // registered at build time by the MongoDB processor, which in turn causes the Flyway
    // processor to create an analytics Flyway bean (inactive when active=false is set).
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
