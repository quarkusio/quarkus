package io.quarkus.liquibase.runtime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LiquibaseProducerTest {

    private static final String DEFAULT_DATASOURCE = "";
    private LiquibaseBuildTimeConfig buildDataSourceConfig = LiquibaseBuildTimeConfig.defaultConfig();
    private LiquibaseRuntimeConfig runtimeDataSourceConfig = LiquibaseRuntimeConfig.defaultConfig();

    /**
     * class under test.
     */
    private LiquibaseProducer liquibaseProducer = new LiquibaseProducer();

    @BeforeEach
    void beforeEach() {
        liquibaseProducer.liquibaseBuildConfig = buildDataSourceConfig;
        liquibaseProducer.liquibaseRuntimeConfig = runtimeDataSourceConfig;
    }

    @Test
    @DisplayName("liquibase can be created successfully")
    void testCreatesLiquibaseSuccessfully() {
        assertNotNull(liquibaseProducer.createLiquibase(null, DEFAULT_DATASOURCE));
    }

    @Test
    @DisplayName("fail on missing build configuration")
    void testMissingBuildConfig() {
        liquibaseProducer.liquibaseBuildConfig = null;
        assertThrows(IllegalStateException.class, () -> liquibaseProducer.createLiquibase(null, DEFAULT_DATASOURCE));
    }

    @Test
    @DisplayName("fail on missing runtime configuration")
    void testMissingRuntimeConfig() {
        liquibaseProducer.liquibaseRuntimeConfig = null;
        assertThrows(IllegalStateException.class, () -> liquibaseProducer.createLiquibase(null, DEFAULT_DATASOURCE));
    }
}