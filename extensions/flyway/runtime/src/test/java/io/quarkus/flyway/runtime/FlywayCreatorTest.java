package io.quarkus.flyway.runtime;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.internal.util.ValidatePatternUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FlywayCreatorTest {

    private FlywayDataSourceRuntimeConfig runtimeConfig = FlywayDataSourceRuntimeConfig.defaultConfig();
    private FlywayDataSourceBuildTimeConfig buildConfig = FlywayDataSourceBuildTimeConfig.defaultConfig();
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
        buildConfig.locations = Arrays.asList("db/migrations", "db/something");
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(buildConfig.locations, pathList(createdFlywayConfig().getLocations()));
    }

    @Test
    @DisplayName("not configured locations replaced by default")
    void testNotPresentLocationsOverridden() {
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

    @Test
    @DisplayName("validate on migrate default matches to true")
    void testValidateOnMigrate() {
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(runtimeConfig.validateOnMigrate, createdFlywayConfig().isValidateOnMigrate());
        assertTrue(runtimeConfig.validateOnMigrate);
    }

    @Test
    @DisplayName("clean disabled default matches to false")
    void testCleanDisabled() {
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(runtimeConfig.cleanDisabled, createdFlywayConfig().isCleanDisabled());
        assertFalse(runtimeConfig.cleanDisabled);

        runtimeConfig.cleanDisabled = false;
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertFalse(createdFlywayConfig().isCleanDisabled());

        runtimeConfig.cleanDisabled = true;
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertTrue(createdFlywayConfig().isCleanDisabled());
    }

    @Test
    @DisplayName("outOfOrder is correctly set")
    void testOutOfOrder() {
        runtimeConfig.outOfOrder = false;
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertFalse(createdFlywayConfig().isOutOfOrder());

        runtimeConfig.outOfOrder = true;
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertTrue(createdFlywayConfig().isOutOfOrder());
    }

    @Test
    @DisplayName("ignoreMissingMigrations is correctly set")
    void testIgnoreMissingMigrations() {
        runtimeConfig.ignoreMissingMigrations = false;
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertFalse(ValidatePatternUtils.isMissingIgnored(createdFlywayConfig().getIgnoreMigrationPatterns()));

        runtimeConfig.ignoreMissingMigrations = true;
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertTrue(ValidatePatternUtils.isMissingIgnored(createdFlywayConfig().getIgnoreMigrationPatterns()));
    }

    @Test
    @DisplayName("ignoreFutureMigrations is correctly set")
    void testIgnoreFutureMigrations() {
        runtimeConfig.ignoreFutureMigrations = false;
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertFalse(ValidatePatternUtils.isFutureIgnored(createdFlywayConfig().getIgnoreMigrationPatterns()));

        runtimeConfig.ignoreFutureMigrations = true;
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertTrue(ValidatePatternUtils.isFutureIgnored(createdFlywayConfig().getIgnoreMigrationPatterns()));
    }

    @Test
    @DisplayName("cleanOnValidationError defaults to false and is correctly set")
    void testCleanOnValidationError() {
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(runtimeConfig.cleanOnValidationError, createdFlywayConfig().isCleanOnValidationError());
        assertFalse(runtimeConfig.cleanOnValidationError);

        runtimeConfig.cleanOnValidationError = false;
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertFalse(createdFlywayConfig().isCleanOnValidationError());

        runtimeConfig.cleanOnValidationError = true;
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertTrue(createdFlywayConfig().isCleanOnValidationError());
    }

    @ParameterizedTest
    @MethodSource("validateOnMigrateOverwritten")
    @DisplayName("validate on migrate overwritten in configuration")
    void testValidateOnMigrateOverwritten(final boolean input, final boolean expected) {
        runtimeConfig.validateOnMigrate = input;
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(createdFlywayConfig().isValidateOnMigrate(), expected);
        assertEquals(runtimeConfig.validateOnMigrate, expected);
    }

    private static List<String> pathList(Location[] locations) {
        return Stream.of(locations).map(Location::getPath).collect(Collectors.toList());
    }

    private Configuration createdFlywayConfig() {
        return creator.createFlyway(null).getConfiguration();
    }

    private static Stream<Arguments> validateOnMigrateOverwritten() {
        return Stream.<Arguments> builder()
                .add(Arguments.arguments(false, false))
                .add(Arguments.arguments(true, true))
                .build();
    }
}
