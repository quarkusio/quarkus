package io.quarkus.flyway.runtime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlywayProducerTest {

    private static final String DEFAULT_DATASOURCE = "";
    private FlywayBuildTimeConfig buildDataSourceConfig = FlywayBuildTimeConfig.defaultConfig();
    private FlywayRuntimeConfig runtimeDataSourceConfig = FlywayRuntimeConfig.defaultConfig();

    /**
     * class under test.
     */
    private FlywayProducer flywayProducer = new FlywayProducer();

    @BeforeEach
    void beforeEach() {
        flywayProducer.flywayBuildConfig = buildDataSourceConfig;
        flywayProducer.flywayRuntimeConfig = runtimeDataSourceConfig;
    }

    @Test
    @DisplayName("flyway can be created successfully")
    void testCreatesFlywaySuccessfully() {
        assertNotNull(flywayProducer.createFlyway(null, DEFAULT_DATASOURCE));
    }

    @Test
    @DisplayName("fail on missing build configuration")
    void testMissingBuildConfig() {
        flywayProducer.flywayBuildConfig = null;
        assertThrows(IllegalStateException.class, () -> flywayProducer.createFlyway(null, DEFAULT_DATASOURCE));
    }

    @Test
    @DisplayName("fail on missing runtime configuration")
    void testMissingRuntimeConfig() {
        flywayProducer.flywayRuntimeConfig = null;
        assertThrows(IllegalStateException.class, () -> flywayProducer.createFlyway(null, DEFAULT_DATASOURCE));
    }
}