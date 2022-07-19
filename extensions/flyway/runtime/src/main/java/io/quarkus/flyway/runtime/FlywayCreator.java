package io.quarkus.flyway.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.configuration.FluentConfiguration;

class FlywayCreator {

    private static final String[] EMPTY_ARRAY = new String[0];

    private final FlywayDataSourceRuntimeConfig flywayRuntimeConfig;
    private final FlywayDataSourceBuildTimeConfig flywayBuildTimeConfig;
    private Collection<Callback> callbacks = Collections.emptyList();

    public FlywayCreator(FlywayDataSourceRuntimeConfig flywayRuntimeConfig,
            FlywayDataSourceBuildTimeConfig flywayBuildTimeConfig) {
        this.flywayRuntimeConfig = flywayRuntimeConfig;
        this.flywayBuildTimeConfig = flywayBuildTimeConfig;
    }

    public FlywayCreator withCallbacks(Collection<Callback> callbacks) {
        this.callbacks = callbacks;
        return this;
    }

    public Flyway createFlyway(DataSource dataSource) {
        FluentConfiguration configure = Flyway.configure();
        configure.dataSource(dataSource);
        if (flywayRuntimeConfig.initSql.isPresent()) {
            configure.initSql(flywayRuntimeConfig.initSql.get());
        }
        if (flywayRuntimeConfig.connectRetries.isPresent()) {
            configure.connectRetries(flywayRuntimeConfig.connectRetries.getAsInt());
        }
        if (flywayRuntimeConfig.defaultSchema.isPresent()) {
            configure.defaultSchema(flywayRuntimeConfig.defaultSchema.get());
        }
        if (flywayRuntimeConfig.schemas.isPresent()) {
            configure.schemas(flywayRuntimeConfig.schemas.get().toArray(EMPTY_ARRAY));
        }
        if (flywayRuntimeConfig.table.isPresent()) {
            configure.table(flywayRuntimeConfig.table.get());
        }
        configure.locations(flywayBuildTimeConfig.locations.toArray(EMPTY_ARRAY));
        if (flywayRuntimeConfig.sqlMigrationPrefix.isPresent()) {
            configure.sqlMigrationPrefix(flywayRuntimeConfig.sqlMigrationPrefix.get());
        }
        if (flywayRuntimeConfig.repeatableSqlMigrationPrefix.isPresent()) {
            configure.repeatableSqlMigrationPrefix(flywayRuntimeConfig.repeatableSqlMigrationPrefix.get());
        }
        configure.cleanDisabled(flywayRuntimeConfig.cleanDisabled);
        configure.baselineOnMigrate(flywayRuntimeConfig.baselineOnMigrate);
        configure.validateOnMigrate(flywayRuntimeConfig.validateOnMigrate);
        List<String> patterns = new ArrayList<>(2);
        //https://flywaydb.org/documentation/configuration/parameters/ignoreMigrationPatterns
        if (flywayRuntimeConfig.ignoreMissingMigrations) {
            patterns.add("*:Missing");
        }
        if (flywayRuntimeConfig.ignoreFutureMigrations) {
            patterns.add("*:Future");
        }
        // Default is *:Future
        configure.ignoreMigrationPatterns(patterns.toArray(new String[0]));
        configure.cleanOnValidationError(flywayRuntimeConfig.cleanOnValidationError);
        configure.outOfOrder(flywayRuntimeConfig.outOfOrder);
        if (flywayRuntimeConfig.baselineVersion.isPresent()) {
            configure.baselineVersion(flywayRuntimeConfig.baselineVersion.get());
        }
        if (flywayRuntimeConfig.baselineDescription.isPresent()) {
            configure.baselineDescription(flywayRuntimeConfig.baselineDescription.get());
        }
        configure.placeholders(flywayRuntimeConfig.placeholders);
        configure.createSchemas(flywayRuntimeConfig.createSchemas);
        if (flywayRuntimeConfig.placeholderPrefix.isPresent()) {
            configure.placeholderPrefix(flywayRuntimeConfig.placeholderPrefix.get());
        }
        if (flywayRuntimeConfig.placeholderSuffix.isPresent()) {
            configure.placeholderSuffix(flywayRuntimeConfig.placeholderSuffix.get());
        }
        if (!callbacks.isEmpty()) {
            configure.callbacks(callbacks.toArray(new Callback[0]));
        }
        /*
         * Ensure that no classpath scanning takes place by setting the ClassProvider and the ResourceProvider
         * (see Flyway#createResourceAndClassProviders)
         */

        // the static fields of this class have already been set at static-init
        QuarkusPathLocationScanner quarkusPathLocationScanner = new QuarkusPathLocationScanner(
                Arrays.asList(configure.getLocations()));
        configure.javaMigrationClassProvider(new QuarkusFlywayClassProvider<>(quarkusPathLocationScanner.scanForClasses()));
        configure.resourceProvider(new QuarkusFlywayResourceProvider(quarkusPathLocationScanner.scanForResources()));

        return configure.load();
    }
}
