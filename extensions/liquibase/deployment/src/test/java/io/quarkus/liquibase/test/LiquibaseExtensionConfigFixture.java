package io.quarkus.liquibase.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;

import io.quarkus.liquibase.runtime.LiquibaseConfig;
import io.quarkus.liquibase.runtime.LiquibaseDataSourceBuildTimeConfig;
import liquibase.GlobalConfiguration;

/**
 * This fixture provides access to read the expected and the actual configuration of liquibase.
 * It also provides a method combining all assertions to be reused for multiple tests.
 */
@ApplicationScoped
public class LiquibaseExtensionConfigFixture {

    @Inject
    Config config;

    public void assertAllConfigurationSettings(LiquibaseConfig configuration, String dataSourceName) {
        assertEquals(configuration.migrateAtStart, migrateAtStart(dataSourceName));
        assertEquals(configuration.cleanAtStart, cleanAtStart(dataSourceName));
        assertEquals(configuration.validateOnMigrate, validateOnMigrate(dataSourceName));
        assertEquals(configuration.changeLog, changeLog(dataSourceName));
        assertEquals(configuration.defaultCatalogName.orElse(null), defaultCatalogName(dataSourceName));
        assertEquals(configuration.defaultSchemaName.orElse(null), defaultSchemaName(dataSourceName));

        assertEquals(configuration.liquibaseCatalogName.orElse(null), liquibaseCatalogName(dataSourceName));
        assertEquals(configuration.liquibaseSchemaName.orElse(null), liquibaseSchemaName(dataSourceName));
        assertEquals(configuration.liquibaseTablespaceName.orElse(null), liquibaseTablespaceName(dataSourceName));

        assertEquals(configuration.databaseChangeLogTableName, databaseChangeLogTableName(dataSourceName));
        assertEquals(configuration.databaseChangeLogLockTableName, databaseChangeLogLockTableName(dataSourceName));
        assertEquals(labels(configuration), labels(dataSourceName));
        assertEquals(contexts(configuration), contexts(dataSourceName));
        assertEquals(changeLogParameters(configuration), changeLogParameters(dataSourceName));
    }

    public void assertDefaultConfigurationSettings(LiquibaseConfig configuration) {

        assertEquals(configuration.changeLog, LiquibaseDataSourceBuildTimeConfig.defaultConfig().changeLog);

        assertEquals(configuration.databaseChangeLogTableName,
                GlobalConfiguration.DATABASECHANGELOG_TABLE_NAME.getCurrentValue());
        assertEquals(configuration.databaseChangeLogLockTableName,
                GlobalConfiguration.DATABASECHANGELOGLOCK_TABLE_NAME.getCurrentValue());
        assertEquals(configuration.liquibaseTablespaceName.orElse(null),
                GlobalConfiguration.LIQUIBASE_TABLESPACE_NAME.getCurrentValue());
        assertEquals(configuration.liquibaseCatalogName.orElse(null),
                GlobalConfiguration.LIQUIBASE_CATALOG_NAME.getCurrentValue());
        assertEquals(configuration.liquibaseSchemaName.orElse(null),
                GlobalConfiguration.LIQUIBASE_SCHEMA_NAME.getCurrentValue());

    }

    public Map<String, String> changeLogParameters(LiquibaseConfig configuration) {
        if (configuration.changeLogParameters == null) {
            return Collections.emptyMap();
        }
        return configuration.changeLogParameters;
    }

    public Map<String, String> changeLogParameters(String datasourceName) {
        String propertyName = fillin("quarkus.liquibase.%s.change-log-parameters", datasourceName);
        Map<String, String> map = new HashMap<>();
        StreamSupport.stream(config.getPropertyNames().spliterator(), false).filter(p -> p.startsWith(propertyName))
                .forEach(p -> map.put(unquote(p.substring(propertyName.length() + 1)), config.getValue(p, String.class)));

        return map;
    }

    private String unquote(String s) {
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        } else {
            return s;
        }
    }

    public String contexts(LiquibaseConfig configuration) {
        if (configuration.contexts == null) {
            return null;
        }
        return String.join(",", configuration.contexts);
    }

    public String contexts(String datasourceName) {
        return getStringValue("quarkus.liquibase.%s.contexts", datasourceName);
    }

    public String labels(LiquibaseConfig configuration) {
        if (configuration.labels == null) {
            return null;
        }
        return String.join(",", configuration.labels);
    }

    public String labels(String datasourceName) {
        return getStringValue("quarkus.liquibase.%s.labels", datasourceName);
    }

    public String changeLog(String datasourceName) {
        return getStringValue("quarkus.liquibase.%s.change-log", datasourceName);
    }

    public String defaultCatalogName(String datasourceName) {
        return getStringValue("quarkus.liquibase.%s.default-catalog-name", datasourceName);
    }

    public String defaultSchemaName(String datasourceName) {
        return getStringValue("quarkus.liquibase.%s.default-schema-name", datasourceName);
    }

    public String liquibaseCatalogName(String datasourceName) {
        return getStringValue("quarkus.liquibase.%s.liquibase-catalog-name", datasourceName);
    }

    public String liquibaseSchemaName(String datasourceName) {
        return getStringValue("quarkus.liquibase.%s.liquibase-schema-name", datasourceName);
    }

    public String liquibaseTablespaceName(String datasourceName) {
        return getStringValue("quarkus.liquibase.%s.liquibase-tablespace-name", datasourceName);
    }

    public String databaseChangeLogTableName(String datasourceName) {
        return getStringValue("quarkus.liquibase.%s.database-change-log-table-name", datasourceName);
    }

    public String databaseChangeLogLockTableName(String datasourceName) {
        return getStringValue("quarkus.liquibase.%s.database-change-log-lock-table-name", datasourceName);
    }

    public boolean migrateAtStart(String datasourceName) {
        return getBooleanValue("quarkus.liquibase.%s.migrate-at-start", datasourceName);
    }

    public boolean cleanAtStart(String datasourceName) {
        return getBooleanValue("quarkus.liquibase.%s.clean-at-start", datasourceName);
    }

    public boolean validateOnMigrate(String datasourceName) {
        return getBooleanValue("quarkus.liquibase.%s.validate-on-migrate", datasourceName);
    }

    private String getStringValue(String parameterName, String datasourceName) {
        return getValue(parameterName, datasourceName, String.class);
    }

    private boolean getBooleanValue(String parameterName, String datasourceName) {
        return getValue(parameterName, datasourceName, Boolean.class);
    }

    private <T> T getValue(String parameterName, String datasourceName, Class<T> type) {
        return getValue(parameterName, datasourceName, type, this::log);
    }

    private <T> T getValue(String parameterName, String datasourceName, Class<T> type, Consumer<String> logger) {
        String propertyName = fillin(parameterName, datasourceName);
        T propertyValue = config.getValue(propertyName, type);
        logger.accept("Config property " + propertyName + " = " + propertyValue);
        return propertyValue;
    }

    private void log(String content) {
        //activate for debugging
        // System.out.println(content);
    }

    private String fillin(String propertyName, String datasourceName) {
        return String.format(propertyName, datasourceName).replace("..", ".");
    }
}
