package io.quarkus.flyway.runtime;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.configuration.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlywayCreatorTest {

    private FlywayDataSourceRuntimeConfig runtimeConfig = FlywayDataSourceRuntimeConfig.defaultConfig();
    private FlywayDataSourceBuildConfig buildConfig = FlywayDataSourceBuildConfig.defaultConfig();
    private Configuration defaultConfig = Flyway.configure().load().getConfiguration();

    /**
     * class under test.
     */
    private FlywayCreator creator;

    @Test
    @DisplayName("locations default matches flyway default")
    void testLocationsDefault() {
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(pathList(defaultConfig.getLocations()), pathList(createdFlywayConfig().getLocations()));
    }

    @Test
    @DisplayName("locations carried over from configuration")
    void testLocationsOverridden() {
        buildConfig.locations = Optional.of(Arrays.asList("db/migrations", "db/something"));
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(buildConfig.locations.get(), pathList(createdFlywayConfig().getLocations()));
    }

    @Test
    @DisplayName("not configured locations replaced by default")
    void testNotPresentLocationsOverridden() {
        buildConfig.locations = Optional.empty();
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(pathList(defaultConfig.getLocations()), pathList(createdFlywayConfig().getLocations()));
    }

    @Test
    @DisplayName("baseline description default matches flyway default")
    void testBaselineDescriptionDefault() {
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(defaultConfig.getBaselineDescription(), createdFlywayConfig().getBaselineDescription());
    }

    @Test
    @DisplayName("baseline description carried over from configuration")
    void testBaselineDescriptionOverridden() {
        runtimeConfig.baselineDescription = Optional.of("baselineDescription");
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(runtimeConfig.baselineDescription.get(), createdFlywayConfig().getBaselineDescription());
    }

    @Test
    @DisplayName("baseline version default matches flyway default")
    void testBaselineVersionDefault() {
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(defaultConfig.getBaselineVersion(), createdFlywayConfig().getBaselineVersion());
    }

    @Test
    @DisplayName("baseline version carried over from configuration")
    void testBaselineVersionOverridden() {
        runtimeConfig.baselineVersion = Optional.of("0.1.2");
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(runtimeConfig.baselineVersion.get(), createdFlywayConfig().getBaselineVersion().getVersion());
    }

    @Test
    @DisplayName("connection retries default matches flyway default")
    void testConnectionRetriesDefault() {
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(defaultConfig.getConnectRetries(), createdFlywayConfig().getConnectRetries());
    }

    @Test
    @DisplayName("connection retries carried over from configuration")
    void testConnectionRetriesOverridden() {
        runtimeConfig.connectRetries = OptionalInt.of(12);
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(runtimeConfig.connectRetries.getAsInt(), createdFlywayConfig().getConnectRetries());
    }

    @Test
    @DisplayName("repeatable SQL migration prefix default matches flyway default")
    void testRepeatableSqlMigrationPrefixDefault() {
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(defaultConfig.getRepeatableSqlMigrationPrefix(), createdFlywayConfig().getRepeatableSqlMigrationPrefix());
    }

    @Test
    @DisplayName("repeatable SQL migration prefix carried over from configuration")
    void testRepeatableSqlMigrationPrefixOverridden() {
        runtimeConfig.repeatableSqlMigrationPrefix = Optional.of("A");
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(runtimeConfig.repeatableSqlMigrationPrefix.get(), createdFlywayConfig().getRepeatableSqlMigrationPrefix());
    }

    @Test
    @DisplayName("schemas default matches flyway default")
    void testSchemasDefault() {
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(asList(defaultConfig.getSchemas()), asList(createdFlywayConfig().getSchemas()));
    }

    @Test
    @DisplayName("schemas carried over from configuration")
    void testSchemasOverridden() {
        runtimeConfig.schemas = Optional.of(Arrays.asList("TEST_SCHEMA_1", "TEST_SCHEMA_2"));
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(runtimeConfig.schemas.get(), asList(createdFlywayConfig().getSchemas()));
    }

    @Test
    @DisplayName("SQL migration prefix default matches flyway default")
    void testSqlMigrationPrefixDefault() {
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(defaultConfig.getSqlMigrationPrefix(), createdFlywayConfig().getSqlMigrationPrefix());
    }

    @Test
    @DisplayName("SQL migration prefix carried over from configuration")
    void testSqlMigrationPrefixOverridden() {
        runtimeConfig.sqlMigrationPrefix = Optional.of("M");
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(runtimeConfig.sqlMigrationPrefix.get(), createdFlywayConfig().getSqlMigrationPrefix());
    }

    @Test
    @DisplayName("table default matches flyway default")
    void testTableDefault() {
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(defaultConfig.getTable(), createdFlywayConfig().getTable());
    }

    @Test
    @DisplayName("table carried over from configuration")
    void testTableOverridden() {
        runtimeConfig.table = Optional.of("flyway_history_test_table");
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(runtimeConfig.table.get(), createdFlywayConfig().getTable());
    }

    private static List<String> pathList(Location[] locations) {
        return Stream.of(locations).map(Location::getPath).collect(Collectors.toList());
    }

    private Configuration createdFlywayConfig() {
        return creator.createFlyway(null).getConfiguration();
    }
}
