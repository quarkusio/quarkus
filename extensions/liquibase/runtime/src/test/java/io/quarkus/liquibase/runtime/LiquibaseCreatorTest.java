package io.quarkus.liquibase.runtime;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.datasource.common.runtime.DataSourceUtil;

class LiquibaseCreatorTest {

    private LiquibaseDataSourceRuntimeConfig runtimeConfig = LiquibaseDataSourceRuntimeConfig.defaultConfig();
    private LiquibaseDataSourceBuildTimeConfig buildConfig = LiquibaseDataSourceBuildTimeConfig.defaultConfig();
    private LiquibaseConfig defaultConfig = new LiquibaseConfig();

    /**
     * class under test.
     */
    private LiquibaseCreator creator;

    @Test
    @DisplayName("changeLog default matches liquibase default")
    void testChangeLogDefault() {
        creator = new LiquibaseCreator(runtimeConfig, buildConfig);
        assertEquals(defaultConfig.changeLog, createdLiquibaseConfig().changeLog);
    }

    @Test
    @DisplayName("changeLog carried over from configuration")
    void testChangeLogOverridden() {
        buildConfig.changeLog = "/db/test.xml";
        creator = new LiquibaseCreator(runtimeConfig, buildConfig);
        assertEquals(buildConfig.changeLog, createdLiquibaseConfig().changeLog);
    }

    @Test
    @DisplayName("migrateAtStart default matches liquibase default")
    void testMigrateAtStartDefault() {
        creator = new LiquibaseCreator(runtimeConfig, buildConfig);
        assertEquals(defaultConfig.migrateAtStart, createdLiquibaseConfig().migrateAtStart);
    }

    @Test
    @DisplayName("migrateAtStart carried over from configuration")
    void testMigrateAtStartOverridden() {
        runtimeConfig.migrateAtStart = true;
        creator = new LiquibaseCreator(runtimeConfig, buildConfig);
        assertTrue(createdLiquibaseConfig().migrateAtStart);
    }

    @Test
    @DisplayName("cleanAtStart default matches liquibase default")
    void testCleanAtStartDefault() {
        creator = new LiquibaseCreator(runtimeConfig, buildConfig);
        assertEquals(defaultConfig.cleanAtStart, createdLiquibaseConfig().cleanAtStart);
    }

    @Test
    @DisplayName("cleanAtStart carried over from configuration")
    void testCleanAtStartOverridden() {
        runtimeConfig.cleanAtStart = true;
        creator = new LiquibaseCreator(runtimeConfig, buildConfig);
        assertTrue(createdLiquibaseConfig().cleanAtStart);
    }

    @Test
    @DisplayName("databaseChangeLogLockTableName default matches liquibase default")
    void testDatabaseChangeLogLockTableNameDefault() {
        creator = new LiquibaseCreator(runtimeConfig, buildConfig);
        assertEquals(defaultConfig.databaseChangeLogLockTableName, createdLiquibaseConfig().databaseChangeLogLockTableName);
    }

    @Test
    @DisplayName("DatabaseChangeLogLockTableName carried over from configuration")
    void testDatabaseChangeLogLockTableNameOverridden() {
        runtimeConfig.databaseChangeLogLockTableName = Optional.of("TEST_LOCK");
        creator = new LiquibaseCreator(runtimeConfig, buildConfig);
        assertEquals(runtimeConfig.databaseChangeLogLockTableName.get(),
                createdLiquibaseConfig().databaseChangeLogLockTableName);
    }

    @Test
    @DisplayName("databaseChangeLogTableName default matches liquibase default")
    void testDatabaseChangeLogTableNameDefault() {
        creator = new LiquibaseCreator(runtimeConfig, buildConfig);
        assertEquals(defaultConfig.databaseChangeLogTableName, createdLiquibaseConfig().databaseChangeLogTableName);
    }

    @Test
    @DisplayName("databaseChangeLogTableName carried over from configuration")
    void testDatabaseChangeLogTableNameOverridden() {
        runtimeConfig.databaseChangeLogLockTableName = Optional.of("TEST_LOG");
        creator = new LiquibaseCreator(runtimeConfig, buildConfig);
        assertEquals(runtimeConfig.databaseChangeLogTableName.get(), createdLiquibaseConfig().databaseChangeLogTableName);
    }

    @Test
    @DisplayName("defaultCatalogName default matches liquibase default")
    void testDefaultCatalogNameDefault() {
        creator = new LiquibaseCreator(runtimeConfig, buildConfig);
        assertEquals(defaultConfig.defaultCatalogName, createdLiquibaseConfig().defaultCatalogName);
    }

    @Test
    @DisplayName("defaultCatalogName carried over from configuration")
    void testDefaultCatalogNameOverridden() {
        runtimeConfig.defaultCatalogName = Optional.of("CATALOG1,CATALOG2");
        creator = new LiquibaseCreator(runtimeConfig, buildConfig);
        assertEquals(runtimeConfig.defaultCatalogName, createdLiquibaseConfig().defaultCatalogName);
    }

    @Test
    @DisplayName("defaultSchemaName default matches liquibase default")
    void testDefaultSchemaNameDefault() {
        creator = new LiquibaseCreator(runtimeConfig, buildConfig);
        assertEquals(defaultConfig.defaultSchemaName, createdLiquibaseConfig().defaultSchemaName);
    }

    @Test
    @DisplayName("defaultSchemaName carried over from configuration")
    void testDefaultSchemaNameOverridden() {
        runtimeConfig.defaultSchemaName = Optional.of("SCHEMA1");
        creator = new LiquibaseCreator(runtimeConfig, buildConfig);
        assertEquals(runtimeConfig.defaultSchemaName, createdLiquibaseConfig().defaultSchemaName);
    }

    @Test
    @DisplayName("contexts default matches liquibase default")
    void testContextsDefault() {
        creator = new LiquibaseCreator(runtimeConfig, buildConfig);
        assertEquals(defaultConfig.contexts, createdLiquibaseConfig().contexts);
    }

    @Test
    @DisplayName("contexts carried over from configuration")
    void testContextsOverridden() {
        runtimeConfig.contexts = Optional.of(Collections.singletonList("CONTEXT1,CONTEXT2"));
        creator = new LiquibaseCreator(runtimeConfig, buildConfig);
        assertIterableEquals(runtimeConfig.contexts.get(), createdLiquibaseConfig().contexts);
    }

    @Test
    @DisplayName("labels default matches liquibase default")
    void testLabelsDefault() {
        creator = new LiquibaseCreator(runtimeConfig, buildConfig);
        assertEquals(defaultConfig.labels, createdLiquibaseConfig().labels);
    }

    @Test
    @DisplayName("labels carried over from configuration")
    void testLabelsOverridden() {
        runtimeConfig.labels = Optional.of(Collections.singletonList("LABEL1,LABEL2"));
        creator = new LiquibaseCreator(runtimeConfig, buildConfig);
        assertIterableEquals(runtimeConfig.labels.get(), createdLiquibaseConfig().labels);
    }

    @Test
    @DisplayName("liquibaseCatalogName default matches liquibase default")
    void testLiquibaseCatalogNameDefault() {
        creator = new LiquibaseCreator(runtimeConfig, buildConfig);
        assertEquals(defaultConfig.liquibaseCatalogName, createdLiquibaseConfig().liquibaseCatalogName);
    }

    @Test
    @DisplayName("defaultCatalogName carried over from configuration")
    void testLiquibaseCatalogNameOverridden() {
        runtimeConfig.liquibaseCatalogName = Optional.of("LIQUIBASE_CATALOG");
        creator = new LiquibaseCreator(runtimeConfig, buildConfig);
        assertEquals(runtimeConfig.liquibaseCatalogName, createdLiquibaseConfig().liquibaseCatalogName);
    }

    @Test
    @DisplayName("liquibaseSchemaName default matches liquibase default")
    void testLiquibaseSchemaNameDefault() {
        creator = new LiquibaseCreator(runtimeConfig, buildConfig);
        assertEquals(defaultConfig.liquibaseSchemaName, createdLiquibaseConfig().liquibaseSchemaName);
    }

    @Test
    @DisplayName("liquibaseSchemaName carried over from configuration")
    void testLiquibaseSchemaNameOverridden() {
        runtimeConfig.liquibaseSchemaName = Optional.of("LIQUIBASE_SCHEMA");
        creator = new LiquibaseCreator(runtimeConfig, buildConfig);
        assertEquals(runtimeConfig.liquibaseSchemaName, createdLiquibaseConfig().liquibaseSchemaName);
    }

    @Test
    @DisplayName("liquibaseTablespaceName default matches liquibase default")
    void testLiquibaseTablespaceNameDefault() {
        creator = new LiquibaseCreator(runtimeConfig, buildConfig);
        assertEquals(defaultConfig.liquibaseTablespaceName, createdLiquibaseConfig().liquibaseTablespaceName);
    }

    @Test
    @DisplayName("liquibaseTablespaceName carried over from configuration")
    void testLiquibaseTablespaceNameOverridden() {
        runtimeConfig.liquibaseSchemaName = Optional.of("LIQUIBASE_SPACE");
        creator = new LiquibaseCreator(runtimeConfig, buildConfig);
        assertEquals(runtimeConfig.liquibaseTablespaceName, createdLiquibaseConfig().liquibaseTablespaceName);
    }

    private LiquibaseConfig createdLiquibaseConfig() {
        return creator.createLiquibaseFactory(null, DataSourceUtil.DEFAULT_DATASOURCE_NAME).getConfiguration();
    }
}
