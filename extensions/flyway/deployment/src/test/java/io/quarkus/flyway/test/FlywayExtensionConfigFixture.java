package io.quarkus.flyway.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.configuration.FluentConfiguration;

/**
 * This fixture provides access to read the expected and the actual configuration of flyway.
 * It also provides a method combining all assertions to be reused for multiple tests.
 */
@ApplicationScoped
public class FlywayExtensionConfigFixture {

    @Inject
    Config config;

    public void assertAllConfigurationSettings(Configuration configuration, String dataSourceName) {
        assertEquals(locations(configuration), locations(dataSourceName));
        assertEquals(sqlMigrationPrefix(configuration), sqlMigrationPrefix(dataSourceName));
        assertEquals(repeatableSqlMigrationPrefix(configuration), repeatableSqlMigrationPrefix(dataSourceName));
        assertEquals(tableName(configuration), tableName(dataSourceName));
        assertEquals(schemaNames(configuration), schemaNames(dataSourceName));

        assertEquals(connectRetries(configuration), connectRetries(dataSourceName));

        assertEquals(baselineOnMigrate(configuration), baselineOnMigrate(dataSourceName));
        assertEquals(baselineVersion(configuration), baselineVersion(dataSourceName));
        assertEquals(baselineDescription(configuration), baselineDescription(dataSourceName));
        assertEquals(callbacks(configuration), callbacks(dataSourceName));
    }

    public void assertDefaultConfigurationSettings(Configuration configuration) {
        FluentConfiguration defaultConfiguration = Flyway.configure();
        assertEquals(locations(configuration), locations(defaultConfiguration));
        assertEquals(sqlMigrationPrefix(configuration), sqlMigrationPrefix(defaultConfiguration));
        assertEquals(repeatableSqlMigrationPrefix(configuration), repeatableSqlMigrationPrefix(defaultConfiguration));
        assertEquals(tableName(configuration), tableName(defaultConfiguration));
        assertEquals(schemaNames(configuration), schemaNames(defaultConfiguration));

        assertEquals(connectRetries(configuration), connectRetries(defaultConfiguration));

        assertEquals(baselineOnMigrate(configuration), baselineOnMigrate(defaultConfiguration));
        assertEquals(baselineVersion(configuration), baselineVersion(defaultConfiguration));
        assertEquals(baselineDescription(configuration), baselineDescription(defaultConfiguration));
        assertEquals(callbacks(configuration), callbacks(defaultConfiguration));
    }

    public int callbacks(Configuration configuration) {
        return configuration.getCallbacks().length;
    }

    public int callbacks(String datasourceName) {
        return getStringValue("quarkus.flyway.%s.callbacks", datasourceName).split(",").length;
    }

    public int connectRetries(String datasourceName) {
        return getIntValue("quarkus.flyway.%s.connect-retries", datasourceName);
    }

    public int connectRetries(Configuration configuration) {
        return configuration.getConnectRetries();
    }

    public String schemaNames(String datasourceName) {
        return getStringValue("quarkus.flyway.%s.schemas", datasourceName);
    }

    public String schemaNames(Configuration configuration) {
        return Arrays.stream(configuration.getSchemas()).collect(Collectors.joining(","));
    }

    public String tableName(String datasourceName) {
        return getStringValue("quarkus.flyway.%s.table", datasourceName);
    }

    public String tableName(Configuration configuration) {
        return configuration.getTable();
    }

    public String locations(String datasourceName) {
        return getStringValue("quarkus.flyway.%s.locations", datasourceName);
    }

    public String locations(Configuration configuration) {
        return Arrays.stream(configuration.getLocations()).map(Location::getPath).collect(Collectors.joining(","));
    }

    public String sqlMigrationPrefix(String datasourceName) {
        return getStringValue("quarkus.flyway.%s.sql-migration-prefix", datasourceName);
    }

    public String sqlMigrationPrefix(Configuration configuration) {
        return configuration.getSqlMigrationPrefix();
    }

    public String repeatableSqlMigrationPrefix(String datasourceName) {
        return getStringValue("quarkus.flyway.%s.repeatable-sql-migration-prefix", datasourceName);
    }

    public String repeatableSqlMigrationPrefix(Configuration configuration) {
        return configuration.getRepeatableSqlMigrationPrefix();
    }

    public boolean baselineOnMigrate(String datasourceName) {
        return getBooleanValue("quarkus.flyway.%s.baseline-on-migrate", datasourceName);
    }

    public boolean baselineOnMigrate(Configuration configuration) {
        return configuration.isBaselineOnMigrate();
    }

    public String baselineVersion(String datasourceName) {
        return getStringValue("quarkus.flyway.%s.baseline-version", datasourceName);
    }

    public String baselineVersion(Configuration configuration) {
        return configuration.getBaselineVersion().getVersion();
    }

    public String baselineDescription(String datasourceName) {
        return getStringValue("quarkus.flyway.%s.baseline-description", datasourceName);
    }

    public String baselineDescription(Configuration configuration) {
        return configuration.getBaselineDescription();
    }

    public boolean migrateAtStart(String datasourceName) {
        return getBooleanValue("quarkus.flyway.migrate-at-start", datasourceName);
    }

    private String getStringValue(String parameterName, String datasourceName) {
        return getValue(parameterName, datasourceName, String.class);
    }

    private int getIntValue(String parameterName, String datasourceName) {
        return getValue(parameterName, datasourceName, Integer.class);
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
