package io.quarkus.flyway.mongodb.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;

import io.quarkus.flyway.mongodb.FlywayMongodbClient;
import io.quarkus.flyway.mongodb.FlywayMongodbConfigurationCustomizer;
import io.quarkus.mongodb.MongoClientName;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that a {@link FlywayMongodbConfigurationCustomizer} qualified with
 * {@code @FlywayMongodbClient("name")} is applied only to that named client's
 * Flyway instance, while an unqualified customizer is applied only to the
 * default client. Exercises the matching logic in
 * {@code FlywayMongodbContainerProducer#matchingCustomizers}.
 */
@ExtendWith(FlapdoodleMongodbExtension.class)
public class FlywayMongodbNamedClientCustomizerTest {

    private static final String DEFAULT_DB = "namedcust_default";
    private static final String ANALYTICS_DB = "namedcust_analytics";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addAsResource("db/migration/V1__create_users.json", "db/migration/V1__create_users.json")
                    .addClasses(DefaultClientCustomizer.class, AnalyticsClientCustomizer.class))
            .overrideConfigKey("quarkus.mongodb.connection-string", FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.database", DEFAULT_DB)
            .overrideConfigKey("quarkus.flyway-mongodb.database", DEFAULT_DB)
            .overrideConfigKey("quarkus.mongodb.analytics.connection-string",
                    FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.analytics.database", ANALYTICS_DB)
            .overrideConfigKey("quarkus.flyway-mongodb.analytics.database", ANALYTICS_DB);

    @Inject
    Flyway defaultFlyway;

    @Inject
    @FlywayMongodbClient("analytics")
    Flyway analyticsFlyway;

    // Inject the named MongoClient so the mongodb-client extension registers the
    // "analytics" client at build time and flyway-mongodb produces a Flyway bean for it.
    @Inject
    @MongoClientName("analytics")
    @SuppressWarnings("unused")
    MongoClient analyticsMongoClient;

    @Test
    void eachCustomizerOnlyTouchesItsOwnClient() {
        // Unqualified customizer applied to the default client only.
        assertThat(defaultFlyway.getConfiguration().getTable()).isEqualTo("default_history");
        // Qualified customizer applied to the analytics client only.
        assertThat(analyticsFlyway.getConfiguration().getTable()).isEqualTo("analytics_history");
    }

    /**
     * No {@link FlywayMongodbClient} qualifier — applies to the default client only.
     */
    @ApplicationScoped
    public static class DefaultClientCustomizer implements FlywayMongodbConfigurationCustomizer {
        @Override
        public void customize(FluentConfiguration configuration) {
            configuration.table("default_history");
        }
    }

    /**
     * Qualified with {@code @FlywayMongodbClient("analytics")} — applies only to
     * the analytics client.
     */
    @ApplicationScoped
    @FlywayMongodbClient("analytics")
    public static class AnalyticsClientCustomizer implements FlywayMongodbConfigurationCustomizer {
        @Override
        public void customize(FluentConfiguration configuration) {
            configuration.table("analytics_history");
        }
    }
}
