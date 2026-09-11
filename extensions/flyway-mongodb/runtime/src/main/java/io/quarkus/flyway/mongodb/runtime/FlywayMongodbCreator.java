package io.quarkus.flyway.mongodb.runtime;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;

import io.quarkus.flyway.mongodb.FlywayMongodbConfigurationCustomizer;

/**
 * Builds a configured {@link Flyway} instance for a single MongoDB client.
 * <p>
 * Calls {@code dataSource(url, user, password)} which sets the URL on Flyway's
 * default environment and, when no JDBC plugin claims the URL, skips
 * DataSource creation so the "native connectors" plugin
 * (flyway-database-nc-mongodb) handles the connection via the official
 * MongoDB driver. The plugin honors {@code user}/{@code password} only when the
 * URL does not already embed credentials; the producer enforces that
 * precedence.
 */
public final class FlywayMongodbCreator {

    private static final String[] EMPTY_ARRAY = new String[0];
    static final Duration DEFAULT_CONNECT_RETRIES_INTERVAL = Duration.ofSeconds(120L);

    private final FlywayMongodbClientRuntimeConfig runtimeConfig;
    private final FlywayMongodbClientBuildTimeConfig buildTimeConfig;
    private final List<FlywayMongodbConfigurationCustomizer> customizers;

    public FlywayMongodbCreator(FlywayMongodbClientRuntimeConfig runtimeConfig,
            FlywayMongodbClientBuildTimeConfig buildTimeConfig,
            List<FlywayMongodbConfigurationCustomizer> customizers) {
        this.runtimeConfig = runtimeConfig;
        this.buildTimeConfig = buildTimeConfig;
        this.customizers = customizers;
    }

    public Flyway createFlyway(String clientName, String connectionString, String user, String password) {
        FluentConfiguration configure = Flyway.configure();

        configure.dataSource(connectionString, user, password);

        if (runtimeConfig.connectRetries().isPresent()) {
            configure.connectRetries(runtimeConfig.connectRetries().getAsInt());
        }
        configure.connectRetriesInterval(
                (int) runtimeConfig.connectRetriesInterval().orElse(DEFAULT_CONNECT_RETRIES_INTERVAL).toSeconds());

        configure.sqlMigrationSuffixes(buildTimeConfig.migrationSuffixes().toArray(EMPTY_ARRAY));
        runtimeConfig.migrationPrefix().ifPresent(configure::sqlMigrationPrefix);
        runtimeConfig.repeatableMigrationPrefix().ifPresent(configure::repeatableSqlMigrationPrefix);

        configure.locations(buildTimeConfig.locations().toArray(EMPTY_ARRAY));

        if (runtimeConfig.collection().isPresent()) {
            configure.table(runtimeConfig.collection().get());
        }
        configure.baselineOnMigrate(runtimeConfig.baselineOnMigrate());
        runtimeConfig.baselineVersion().ifPresent(configure::baselineVersion);
        runtimeConfig.baselineDescription().ifPresent(configure::baselineDescription);
        configure.validateOnMigrate(runtimeConfig.validateOnMigrate());
        configure.validateMigrationNaming(runtimeConfig.validateMigrationNaming());
        configure.outOfOrder(runtimeConfig.outOfOrder());
        configure.cleanDisabled(runtimeConfig.cleanDisabled());

        String[] ignoreMigrationPatterns;
        if (runtimeConfig.ignoreMigrationPatterns().isPresent()) {
            ignoreMigrationPatterns = runtimeConfig.ignoreMigrationPatterns().get();
        } else {
            List<String> patterns = new ArrayList<>(2);
            if (runtimeConfig.ignoreMissingMigrations()) {
                patterns.add("*:Missing");
            }
            if (runtimeConfig.ignoreFutureMigrations()) {
                patterns.add("*:Future");
            }
            // Default is *:Future
            ignoreMigrationPatterns = patterns.toArray(EMPTY_ARRAY);
        }
        configure.ignoreMigrationPatterns(ignoreMigrationPatterns);

        configure.placeholders(runtimeConfig.placeholders());
        runtimeConfig.placeholderPrefix().ifPresent(configure::placeholderPrefix);
        runtimeConfig.placeholderSuffix().ifPresent(configure::placeholderSuffix);

        configure.encoding(StandardCharsets.UTF_8);
        configure.detectEncoding(false);
        configure.failOnMissingLocations(false);

        for (FlywayMongodbConfigurationCustomizer customizer : customizers) {
            customizer.customize(configure);
        }

        return configure.load();
    }
}
