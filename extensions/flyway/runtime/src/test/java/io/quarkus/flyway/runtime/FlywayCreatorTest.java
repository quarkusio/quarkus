package io.quarkus.flyway.runtime;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
import org.flywaydb.core.api.pattern.ValidatePattern;
import org.flywaydb.core.internal.util.ValidatePatternUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class FlywayCreatorTest {

    private FlywayDataSourceRuntimeConfig runtimeConfig;
    private FlywayDataSourceBuildTimeConfig buildConfig;
    private final Configuration defaultConfig = Flyway.configure().load().getConfiguration();

    /**
     * class under test.
     */
    private FlywayCreator creator;

    @BeforeEach
    void mockConfig() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().addDiscoveredSources().addDefaultSources().addDiscoveredConverters()
                .withMapping(FlywayRuntimeConfig.class, "quarkus.flyway")
                .withMapping(FlywayBuildTimeConfig.class, "quarkus.flyway")
                .build();

        FlywayRuntimeConfig flywayRuntimeConfig = config.getConfigMapping(FlywayRuntimeConfig.class);
        FlywayBuildTimeConfig flywayBuildTimeConfig = config.getConfigMapping(FlywayBuildTimeConfig.class);
        this.runtimeConfig = Mockito.spy(flywayRuntimeConfig.defaultDataSource());
        this.buildConfig = Mockito.spy(flywayBuildTimeConfig.defaultDataSource());
    }

    @Test
    @DisplayName("locations default matches flyway default")
    void testLocationsDefault() {
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(pathList(defaultConfig.getLocations()), pathList(createdFlywayConfig().getLocations()));
    }

    @Test
    @DisplayName("locations carried over from configuration")
    void testLocationsOverridden() {
        Mockito.when(buildConfig.locations()).thenReturn(Arrays.asList("db/migrations", "db/something"));
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(buildConfig.locations(), pathList(createdFlywayConfig().getLocations()));
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
        Mockito.when(runtimeConfig.baselineDescription()).thenReturn(Optional.of("baselineDescription"));
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(runtimeConfig.baselineDescription().get(), createdFlywayConfig().getBaselineDescription());
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
        Mockito.when(runtimeConfig.baselineVersion()).thenReturn(Optional.of("0.1.2"));
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(runtimeConfig.baselineVersion().get(), createdFlywayConfig().getBaselineVersion().getVersion());
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
        Mockito.when(runtimeConfig.connectRetries()).thenReturn(OptionalInt.of(12));
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(runtimeConfig.connectRetries().getAsInt(), createdFlywayConfig().getConnectRetries());
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
        Mockito.when(runtimeConfig.repeatableSqlMigrationPrefix()).thenReturn(Optional.of("A"));
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(runtimeConfig.repeatableSqlMigrationPrefix().get(),
                createdFlywayConfig().getRepeatableSqlMigrationPrefix());
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
        Mockito.when(runtimeConfig.schemas()).thenReturn(Optional.of(asList("TEST_SCHEMA_1", "TEST_SCHEMA_2")));
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(runtimeConfig.schemas().get(), asList(createdFlywayConfig().getSchemas()));
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
        Mockito.when(runtimeConfig.sqlMigrationPrefix()).thenReturn(Optional.of("A"));
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(runtimeConfig.sqlMigrationPrefix().get(), createdFlywayConfig().getSqlMigrationPrefix());
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
        Mockito.when(runtimeConfig.table()).thenReturn(Optional.of("flyway_history_test_table"));
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(runtimeConfig.table().get(), createdFlywayConfig().getTable());
    }

    @Test
    @DisplayName("validate on migrate default matches to true")
    void testValidateOnMigrate() {
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(runtimeConfig.validateOnMigrate(), createdFlywayConfig().isValidateOnMigrate());
        assertTrue(runtimeConfig.validateOnMigrate());
    }

    @Test
    @DisplayName("clean disabled default matches to false")
    void testCleanDisabled() {
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(runtimeConfig.cleanDisabled(), createdFlywayConfig().isCleanDisabled());
        assertFalse(runtimeConfig.cleanDisabled());

        Mockito.when(runtimeConfig.cleanDisabled()).thenReturn(false);
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertFalse(createdFlywayConfig().isCleanDisabled());

        Mockito.when(runtimeConfig.cleanDisabled()).thenReturn(true);
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertTrue(createdFlywayConfig().isCleanDisabled());
    }

    @Test
    @DisplayName("outOfOrder is correctly set")
    void testOutOfOrder() {
        Mockito.when(runtimeConfig.outOfOrder()).thenReturn(false);
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertFalse(createdFlywayConfig().isOutOfOrder());

        Mockito.when(runtimeConfig.outOfOrder()).thenReturn(true);
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertTrue(createdFlywayConfig().isOutOfOrder());
    }

    @Test
    @DisplayName("ignoreMissingMigrations is correctly set")
    void testIgnoreMissingMigrations() {
        Mockito.when(runtimeConfig.ignoreMissingMigrations()).thenReturn(false);
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertFalse(ValidatePatternUtils.isMissingIgnored(createdFlywayConfig().getIgnoreMigrationPatterns()));

        Mockito.when(runtimeConfig.ignoreMissingMigrations()).thenReturn(true);
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertTrue(ValidatePatternUtils.isMissingIgnored(createdFlywayConfig().getIgnoreMigrationPatterns()));
    }

    @Test
    @DisplayName("ignoreFutureMigrations is correctly set")
    void testIgnoreFutureMigrations() {
        Mockito.when(runtimeConfig.ignoreFutureMigrations()).thenReturn(false);
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertFalse(ValidatePatternUtils.isFutureIgnored(createdFlywayConfig().getIgnoreMigrationPatterns()));

        Mockito.when(runtimeConfig.ignoreFutureMigrations()).thenReturn(true);
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertTrue(ValidatePatternUtils.isFutureIgnored(createdFlywayConfig().getIgnoreMigrationPatterns()));
    }

    @Test
    @DisplayName("cleanOnValidationError defaults to false and is correctly set")
    void testCleanOnValidationError() {
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(runtimeConfig.cleanOnValidationError(), createdFlywayConfig().isCleanOnValidationError());
        assertFalse(runtimeConfig.cleanOnValidationError());

        Mockito.when(runtimeConfig.cleanOnValidationError()).thenReturn(false);
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertFalse(createdFlywayConfig().isCleanOnValidationError());

        Mockito.when(runtimeConfig.cleanOnValidationError()).thenReturn(true);
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertTrue(createdFlywayConfig().isCleanOnValidationError());
    }

    @ParameterizedTest
    @MethodSource("validateOnMigrateOverwritten")
    @DisplayName("validate on migrate overwritten in configuration")
    void testValidateOnMigrateOverwritten(final boolean input, final boolean expected) {
        Mockito.when(runtimeConfig.validateOnMigrate()).thenReturn(input);
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(createdFlywayConfig().isValidateOnMigrate(), expected);
        assertEquals(runtimeConfig.validateOnMigrate(), expected);
    }

    @Test
    @DisplayName("validateMigrationNaming defaults to false and it is correctly set")
    void testValidateMigrationNaming() {
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(runtimeConfig.validateMigrationNaming(), createdFlywayConfig().isValidateMigrationNaming());
        assertFalse(runtimeConfig.validateMigrationNaming());

        Mockito.when(runtimeConfig.validateMigrationNaming()).thenReturn(true);
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertTrue(createdFlywayConfig().isValidateMigrationNaming());
    }

    @Test
    @DisplayName("validateIgnoreMigrationPatterns defaults to false and it is correctly set")
    void testIgnoreMigrationPatterns() {
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        assertEquals(0, createdFlywayConfig().getIgnoreMigrationPatterns().length);
        assertFalse(runtimeConfig.ignoreMigrationPatterns().isPresent());

        Mockito.when(runtimeConfig.ignoreMigrationPatterns()).thenReturn(Optional.of(new String[] { "*:missing" }));
        creator = new FlywayCreator(runtimeConfig, buildConfig);
        final ValidatePattern[] existingIgnoreMigrationPatterns = createdFlywayConfig().getIgnoreMigrationPatterns();
        assertEquals(1, existingIgnoreMigrationPatterns.length);
        final String[] ignoreMigrationPatterns = runtimeConfig.ignoreMigrationPatterns().get();
        final ValidatePattern[] validatePatterns = Arrays.stream(ignoreMigrationPatterns)
                .map(ValidatePattern::fromPattern).toArray(ValidatePattern[]::new);
        assertArrayEquals(validatePatterns, existingIgnoreMigrationPatterns);
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
