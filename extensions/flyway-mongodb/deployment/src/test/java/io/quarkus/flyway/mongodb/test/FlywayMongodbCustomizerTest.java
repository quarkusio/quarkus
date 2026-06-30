package io.quarkus.flyway.mongodb.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.flyway.mongodb.FlywayMongodbConfigurationCustomizer;
import io.quarkus.test.QuarkusExtensionTest;

@ExtendWith(FlapdoodleMongodbExtension.class)
public class FlywayMongodbCustomizerTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/migration/V1__create_users.json", "db/migration/V1__create_users.json")
                    .addClasses(TableNameCustomizer.class))
            .overrideConfigKey("quarkus.mongodb.connection-string", FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.database", "customizer")
            .overrideConfigKey("quarkus.flyway-mongodb.migrate-at-start", "true")
            .overrideConfigKey("quarkus.flyway-mongodb.database", "customizer")
            .overrideConfigKey("quarkus.flyway-mongodb.migration-suffixes", ".json");

    @Inject
    Flyway flyway;

    @Test
    void customizerIsApplied() {
        assertThat(flyway.getConfiguration().getTable()).isEqualTo("custom_flyway_history");
    }

    @ApplicationScoped
    public static class TableNameCustomizer implements FlywayMongodbConfigurationCustomizer {
        @Override
        public void customize(FluentConfiguration configuration) {
            configuration.table("custom_flyway_history");
        }
    }
}
