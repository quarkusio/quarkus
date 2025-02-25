package io.quarkus.flyway.runtime;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.configuration.FluentConfiguration;

import io.agroal.api.AgroalDataSource;
import io.quarkus.flyway.FlywayConfigurationCustomizer;
import io.quarkus.runtime.configuration.ConfigurationException;

class FlywayCreator {

    private static final String[] EMPTY_ARRAY = new String[0];
    public static final Duration DEFAULT_CONNECT_RETRIES_INTERVAL = Duration.ofSeconds(120L);

    private final FlywayDataSourceRuntimeConfig flywayRuntimeConfig;
    private final FlywayDataSourceBuildTimeConfig flywayBuildTimeConfig;
    private final List<FlywayConfigurationCustomizer> customizers;
    private Collection<Callback> callbacks = Collections.emptyList();

    // only used for tests
    public FlywayCreator(FlywayDataSourceRuntimeConfig flywayRuntimeConfig,
            FlywayDataSourceBuildTimeConfig flywayBuildTimeConfig) {
        this.flywayRuntimeConfig = flywayRuntimeConfig;
        this.flywayBuildTimeConfig = flywayBuildTimeConfig;
        this.customizers = Collections.emptyList();
    }

    public FlywayCreator(FlywayDataSourceRuntimeConfig flywayRuntimeConfig,
            FlywayDataSourceBuildTimeConfig flywayBuildTimeConfig,
            List<FlywayConfigurationCustomizer> customizers) {
        this.flywayRuntimeConfig = flywayRuntimeConfig;
        this.flywayBuildTimeConfig = flywayBuildTimeConfig;
        this.customizers = customizers;
    }

    public FlywayCreator withCallbacks(Collection<Callback> callbacks) {
        this.callbacks = callbacks;
        return this;
    }

    public Flyway createFlyway(DataSource dataSource) {
        FluentConfiguration configure = Flyway.configure();

        if (flywayRuntimeConfig.jdbcUrl().isPresent()) {
            if (flywayRuntimeConfig.username().isPresent() && flywayRuntimeConfig.password().isPresent()) {
                configure.dataSource(flywayRuntimeConfig.jdbcUrl().get(), flywayRuntimeConfig.username().get(),
                        flywayRuntimeConfig.password().get());
            } else {
                throw new ConfigurationException(
                        "Username and password must be defined when a JDBC URL is provided in the Flyway configuration");
            }
        } else {
            if (flywayRuntimeConfig.username().isPresent() && flywayRuntimeConfig.password().isPresent()) {
                AgroalDataSource agroalDataSource = (AgroalDataSource) dataSource;
                String jdbcUrl = agroalDataSource.getConfiguration().connectionPoolConfiguration()
                        .connectionFactoryConfiguration().jdbcUrl();

                configure.dataSource(jdbcUrl, flywayRuntimeConfig.username().get(),
                        flywayRuntimeConfig.password().get());
            } else if (dataSource != null) {
                configure.dataSource(dataSource);
            }
        }
        if (flywayRuntimeConfig.initSql().isPresent()) {
            configure.initSql(flywayRuntimeConfig.initSql().get());
        }
        if (flywayRuntimeConfig.connectRetries().isPresent()) {
            configure.connectRetries(flywayRuntimeConfig.connectRetries().getAsInt());
        }
        configure.connectRetriesInterval(
                (int) flywayRuntimeConfig.connectRetriesInterval().orElse(DEFAULT_CONNECT_RETRIES_INTERVAL).toSeconds());
        if (flywayRuntimeConfig.defaultSchema().isPresent()) {
            configure.defaultSchema(flywayRuntimeConfig.defaultSchema().get());
        }
        if (flywayRuntimeConfig.schemas().isPresent()) {
            configure.schemas(flywayRuntimeConfig.schemas().get().toArray(EMPTY_ARRAY));
        }
        if (flywayRuntimeConfig.table().isPresent()) {
            configure.table(flywayRuntimeConfig.table().get());
        }
        configure.locations(flywayBuildTimeConfig.locations().toArray(EMPTY_ARRAY));
        if (flywayRuntimeConfig.sqlMigrationPrefix().isPresent()) {
            configure.sqlMigrationPrefix(flywayRuntimeConfig.sqlMigrationPrefix().get());
        }
        if (flywayRuntimeConfig.repeatableSqlMigrationPrefix().isPresent()) {
            configure.repeatableSqlMigrationPrefix(flywayRuntimeConfig.repeatableSqlMigrationPrefix().get());
        }
        configure.cleanDisabled(flywayRuntimeConfig.cleanDisabled());
        configure.baselineOnMigrate(flywayRuntimeConfig.baselineOnMigrate());
        configure.validateOnMigrate(flywayRuntimeConfig.validateOnMigrate());
        configure.validateMigrationNaming(flywayRuntimeConfig.validateMigrationNaming());

        final String[] ignoreMigrationPatterns;
        if (flywayRuntimeConfig.ignoreMigrationPatterns().isPresent()) {
            ignoreMigrationPatterns = flywayRuntimeConfig.ignoreMigrationPatterns().get();
        } else {
            List<String> patterns = new ArrayList<>(2);
            if (flywayRuntimeConfig.ignoreMissingMigrations()) {
                patterns.add("*:Missing");
            }
            if (flywayRuntimeConfig.ignoreFutureMigrations()) {
                patterns.add("*:Future");
            }
            // Default is *:Future
            ignoreMigrationPatterns = patterns.toArray(new String[0]);
        }

        configure.ignoreMigrationPatterns(ignoreMigrationPatterns);
        configure.outOfOrder(flywayRuntimeConfig.outOfOrder());
        if (flywayRuntimeConfig.baselineVersion().isPresent()) {
            configure.baselineVersion(flywayRuntimeConfig.baselineVersion().get());
        }
        if (flywayRuntimeConfig.baselineDescription().isPresent()) {
            configure.baselineDescription(flywayRuntimeConfig.baselineDescription().get());
        }
        configure.placeholders(flywayRuntimeConfig.placeholders());
        configure.createSchemas(flywayRuntimeConfig.createSchemas());
        if (flywayRuntimeConfig.placeholderPrefix().isPresent()) {
            configure.placeholderPrefix(flywayRuntimeConfig.placeholderPrefix().get());
        }
        if (flywayRuntimeConfig.placeholderSuffix().isPresent()) {
            configure.placeholderSuffix(flywayRuntimeConfig.placeholderSuffix().get());
        }
        if (!callbacks.isEmpty()) {
            configure.callbacks(callbacks.toArray(new Callback[0]));
        }
        /*
         * Ensure that no classpath scanning takes place by setting the ClassProvider and the ResourceProvider
         * (see Flyway#createResourceAndClassProviders)
         */

        // this configuration is important for the scanner
        configure.encoding(StandardCharsets.UTF_8);
        configure.detectEncoding(false);
        configure.failOnMissingLocations(false);

        // the static fields of this class have already been set at static-init
        QuarkusPathLocationScanner quarkusPathLocationScanner = new QuarkusPathLocationScanner(
                configure, Arrays.asList(configure.getLocations()));
        configure.javaMigrationClassProvider(new QuarkusFlywayClassProvider<>(quarkusPathLocationScanner.scanForClasses()));
        configure.resourceProvider(new QuarkusFlywayResourceProvider(quarkusPathLocationScanner.scanForResources()));

        for (FlywayConfigurationCustomizer customizer : customizers) {
            customizer.customize(configure);
        }

        return configure.load();
    }
}
